package slimeknights.tconstruct.library.recipe.molding;

import slimeknights.mantle.recipe.helper.IngredientHelper;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICommonRecipe;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;

/** Recipe to combine two items on the top of a casting table, changing the first */
public class MoldingRecipe implements ICommonRecipe<IMoldingContainer> {
  public static final RecordLoadable<MoldingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
    ContextKey.ID.requiredField(),
    IngredientLoadable.DISALLOW_EMPTY.requiredField("material", MoldingRecipe::getMaterial),
    IngredientLoadable.ALLOW_EMPTY.defaultField("pattern", IngredientHelper.EMPTY, MoldingRecipe::getPattern),
    BooleanLoadable.INSTANCE.defaultField("pattern_consumed", false, false, MoldingRecipe::isPatternConsumed),
    ItemOutput.Loadable.REQUIRED_ITEM.requiredField("result", r -> r.recipeOutput),
    MoldingRecipe::new);

  @Getter
  @SuppressWarnings("rawtypes")  // raw so the lombok getter satisfies Recipe#getType bounds
  private final RecipeType type;
  @Getter
  @SuppressWarnings("rawtypes")  // raw so the lombok getter satisfies Recipe#getSerializer bounds
  private final RecipeSerializer serializer;
  @Getter
  private final Identifier id;
  @Getter
  private final Ingredient material;
  @Getter
  private final Ingredient pattern;
  @Getter
  private final boolean patternConsumed;
  private final ItemOutput recipeOutput;

  public MoldingRecipe(TypeAwareRecipeSerializer<?> serializer, Identifier id, Ingredient material, Ingredient pattern, boolean patternConsumed, ItemOutput recipeOutput) {
    this.type = serializer.getType();
    this.serializer = serializer;
    this.id = id;
    this.material = material;
    this.pattern = pattern;
    this.patternConsumed = !IngredientHelper.isEmpty(pattern) && patternConsumed;
    this.recipeOutput = recipeOutput;
  }

  @Override
  public boolean matches(IMoldingContainer inv, Level worldIn) {
    return material.test(inv.getMaterial()) && pattern.test(inv.getPattern());
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(IngredientHelper.EMPTY, material, pattern);
  }

  @Override
  public ItemStack getResultItem(RegistryAccess access) {
    return recipeOutput.get();
  }
}
