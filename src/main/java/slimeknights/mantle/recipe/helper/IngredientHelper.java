package slimeknights.mantle.recipe.helper;

import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;
import slimeknights.mantle.util.ValueIOHelper;

import java.util.List;

/** Helpers replacing ingredient APIs removed in 1.21.2 */
public final class IngredientHelper {
  private IngredientHelper() {}

  /** Ingredient matching only empty stacks, replacement for {@code IngredientHelper.EMPTY} */
  public static final Ingredient EMPTY = EmptyIngredient.INSTANCE.toVanilla();

  /** Creates an ingredient matching the given tag. Unlike looking the tag up in the registry, this works before tags load and for missing tags. */
  public static Ingredient of(TagKey<Item> tag) {
    return Ingredient.of(tagHolder(BuiltInRegistries.ITEM, tag));
  }

  /**
   * Gets the holder set the registry binds when tags load, creating it if needed.
   * Unlike {@link net.minecraft.core.Registry#get(TagKey)} this works before tags load and for tags without a file.
   */
  public static <T> HolderSet.Named<T> tagHolder(Registry<T> registry, TagKey<T> tag) {
    if (registry instanceof MappedRegistry<T> mapped) {
      synchronized (mapped) {
        return mapped.getOrCreateTagForRegistration(tag);
      }
    }
    return registry.get(tag).orElseGet(() -> HolderSet.emptyNamed(registry, tag));
  }

  /** Checks if the ingredient is {@link #EMPTY} or otherwise matches nothing */
  public static boolean isEmpty(Ingredient ingredient) {
    return ingredient.getCustomIngredient() == EmptyIngredient.INSTANCE || ingredient.isEmpty();
  }

  /** Gets the stacks to display for the ingredient, replacement for {@code Ingredient#getItems()} */
  public static ItemStack[] getItems(Ingredient ingredient) {
    return getDisplayItems(ingredient).toArray(ItemStack[]::new);
  }

  /** Gets the stacks to display for the ingredient */
  public static List<ItemStack> getDisplayItems(Ingredient ingredient) {
    if (ingredient.getCustomIngredient() == EmptyIngredient.INSTANCE) {
      return List.of();
    }
    if (!ingredient.isCustom()) {
      return ingredient.items().map(ItemStack::new).toList();
    }
    ContextMap context = new ContextMap.Builder()
      .withParameter(SlotDisplayContext.REGISTRIES, ValueIOHelper.registries(null))
      .create(SlotDisplayContext.CONTEXT);
    return ingredient.display().resolveForStacks(context);
  }

  /** Gets the registry key used by item tags, for code constructing tags manually */
  public static TagKey<Item> itemTag(net.minecraft.resources.Identifier id) {
    return TagKey.create(Registries.ITEM, id);
  }
}
