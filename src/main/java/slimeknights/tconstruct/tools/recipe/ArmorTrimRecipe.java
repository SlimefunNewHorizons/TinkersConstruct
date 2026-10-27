package slimeknights.tconstruct.tools.recipe;

import java.util.stream.Stream;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.RecipeAccessHelper;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.Holder;
import lombok.Getter;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.cosmetic.TrimModule;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ArmorTrimRecipe implements ITinkerStationRecipe, IMultiRecipe<IDisplayModifierRecipe> {
  protected static final String KEY_INVALID_MATERIAL = TConstruct.makeTranslationKey("recipe", "modifier.armor_trim.invalid_material");
  protected static final String KEY_INVALID_PATTERN = TConstruct.makeTranslationKey("recipe", "modifier.armor_trim.invalid_pattern");


  @Getter
  private final Identifier id;

  public ArmorTrimRecipe(Identifier id) {
    this.id = id;
    ModifierRecipeLookup.addRecipeModifier(null, TinkerModifiers.trim);
  }

  /** Finds the trim pattern for the template, templates are defined by smithing trim recipes since 1.21.5 */
  @Nullable
  private static Holder<TrimPattern> findPattern(ItemStack template) {
    for (RecipeHolder<SmithingRecipe> holder : RecipeAccessHelper.byType(null, RecipeType.SMITHING)) {
      if (holder.value() instanceof SmithingTrimRecipe trim && trim.templateIngredient().map(ingredient -> ingredient.test(template)).orElse(false)) {
        return trim.pattern;
      }
    }
    return null;
  }

  /** Gets all trim templates */
  private static List<ItemStack> getTemplates() {
    return RecipeAccessHelper.byType(null, RecipeType.SMITHING).stream()
      .flatMap(holder -> holder.value() instanceof SmithingTrimRecipe trim ? trim.templateIngredient().stream() : Stream.empty())
      .flatMap(ingredient -> IngredientHelper.getDisplayItems(ingredient).stream())
      .toList();
  }

  /** Match for the trim item finding method */
  private record TrimItems(ItemStack template, ItemStack material) {}

  /** Finds the trim template and material */
  @Nullable
  private static TrimItems findInputs(ITinkerStationContainer inv) {
    ItemStack template = ItemStack.EMPTY;
    ItemStack material = ItemStack.EMPTY;
    for (int i = 0; i < inv.getInputCount(); i++) {
      ItemStack stack = inv.getInput(i);
      if (!stack.isEmpty()) {
        // find the two matching tags, but ensure no duplicates
        if (findPattern(stack) != null) {
          if (!template.isEmpty()) {
            return null;
          }
          template = stack;
        }
        if (stack.is(ItemTags.TRIM_MATERIALS)) {
          if (!material.isEmpty()) {
            return null;
          }
          material = stack;
        }
      }
    }
    // if we found both, we match
    if (!material.isEmpty() && !template.isEmpty()) {
      return new TrimItems(template, material);
    }
    return null;
  }

  @Override
  public boolean matches(ITinkerStationContainer inv, Level world) {
    // ensure this modifier can be applied
    if (!inv.getTinkerableStack().is(TinkerTags.Items.TRIM)) {
      return false;
    }
    // need to locate two things: the trim material, and the trim template
    return findInputs(inv) != null;
  }

  @Override
  public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer inv, RegistryAccess access) {
    // first need to find our trim and material instances
    TrimItems trimItems = findInputs(inv);
    // should never happen
    if (trimItems == null) {
      return RecipeResult.pass();
    }
    // validate the material nad pattern items
    Holder<TrimMaterial> material = TrimMaterials.getFromIngredient(access, trimItems.material).orElse(null);
    if (material == null) {
      return RecipeResult.failure(KEY_INVALID_MATERIAL, trimItems.material.getDisplayName());
    }
    ToolStack original = inv.getTinkerable();
    Holder<TrimPattern> pattern = null;
    if (!original.hasTag(TinkerTags.Items.TRIM_NO_PATTERN)) {
      pattern = findPattern(trimItems.template);
      if (pattern == null) {
        return RecipeResult.failure(KEY_INVALID_PATTERN, trimItems.template.getDisplayName());
      }
    }

    // store into tool NBT
    ToolStack tool = inv.getTinkerable().copy();
    ModDataNBT persistentData = tool.getPersistentData();
    ModifierId modifier = TinkerModifiers.trim.getId();
    persistentData.putString(TrimModule.materialKey(modifier), material.unwrapKey().orElseThrow().identifier().toString());
    if (pattern != null) {
      persistentData.putString(TrimModule.patternKey(modifier), pattern.unwrapKey().orElseThrow().identifier().toString());
    }

    // add the modifier if missing
    if (tool.getModifierLevel(modifier) == 0) {
      tool.addModifier(modifier, 1);
    }
    return ITinkerStationRecipe.success(tool, inv);
  }

  @Override
  public RecipeSerializer getSerializer() {
    return TinkerModifiers.armorTrimSerializer.get();
  }


  /* JEI */

  private List<IDisplayModifierRecipe> displayRecipes = null;

  @SuppressWarnings("deprecation")
  @Override
  public List<IDisplayModifierRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      List<ItemStack> trims = getTemplates();
      List<ItemStack> toolInputs = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, TinkerTags.Items.TRIM)
                                                 .map(IModifiableDisplay::getDisplayStack).toList();
      if (!trims.isEmpty() && !toolInputs.isEmpty()) {
        Identifier id = getId();
        displayRecipes = access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements()
          .map(material -> new DisplayRecipe(id, toolInputs, trims, material, access))
          .collect(Collectors.toList());
      } else {
        displayRecipes = List.of();
      }
    }
    return displayRecipes;
  }

  private static class DisplayRecipe implements IDisplayModifierRecipe {
    private static final IntRange LEVELS = new IntRange(1, 1);
    private final ModifierEntry RESULT = new ModifierEntry(TinkerModifiers.trim, 1);

    @Getter
    private final Identifier recipeId;
    @Getter
    private final List<ItemStack> toolWithoutModifier;
    @Getter
    private final List<ItemStack> toolWithModifier;
    private final List<ItemStack> trim;
    private final List<ItemStack> material;
    @Getter
    private final Component variant;
    private final Identifier materialKey;
    private final String materialName;

    public DisplayRecipe(Identifier id, List<ItemStack> tools, List<ItemStack> trim, Reference<TrimMaterial> holder, RegistryAccess access) {
      this.recipeId = id;
      TrimMaterial material = holder.value();
      toolWithoutModifier = tools;
      this.trim = trim;
      // materials are defined by an item component since 1.21.5
      this.material = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, ItemTags.TRIM_MATERIALS).map(ItemStack::new)
        .filter(stack -> TrimMaterials.getFromIngredient(access, stack).filter(found -> found.is(holder)).isPresent()).toList();
      this.variant = material.description().plainCopy();

      this.materialName = holder.key().identifier().toString();
      List<ModifierEntry> results = List.of(RESULT);
      this.materialKey = TrimModule.materialKey(TinkerModifiers.trim.getId());
      toolWithModifier = tools.stream().map(stack -> IDisplayModifierRecipe.withModifiers(stack, results, data -> data.putString(materialKey, materialName))).toList();

    }

    @Override
    public ModifierEntry getDisplayResult() {
      return RESULT;
    }

    @Override
    public ItemStack getToolWithModifier(ItemStack tool) {
      ToolStack toolStack = ToolStack.from(IDisplayModifierRecipe.super.getToolWithModifier(tool));
      toolStack.getPersistentData().putString(materialKey, materialName);
      return toolStack.createStack(1);
    }

    @Override
    public int getInputCount() {
      return 2;
    }

    @Override
    public List<ItemStack> getDisplayItems(int slot) {
      return switch (slot) {
        case 0 -> trim;
        case 1 -> material;
        default -> List.of();
      };
    }

    @Override
    public IntRange getLevel() {
      return LEVELS;
    }
  }
}
