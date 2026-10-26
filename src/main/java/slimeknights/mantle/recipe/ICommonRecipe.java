package slimeknights.mantle.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Extension of {@link Recipe} to set some methods that always set.
 * Also keeps the display methods {@link Recipe} lost in 1.21.2, as JEI and our books still use them.
 * @param <C>  Inventory type
 */
public interface ICommonRecipe<C extends RecipeInput> extends Recipe<C> {
  @Override
  default ItemStack assemble(C inv, HolderLookup.Provider access) {
    if (access instanceof RegistryAccess registryAccess) {
      return assemble(inv, registryAccess);
    }
    return getResultItem(access).copy();
  }

  /** Transitional bridge for recipes still using the pre-1.21 registry access signature. */
  @Deprecated
  default ItemStack assemble(C inv, RegistryAccess access) {
    return getResultItem(access).copy();
  }

  /** Gets the result for display, removed from {@link Recipe} in 1.21.2 */
  default ItemStack getResultItem(HolderLookup.Provider access) {
    if (access instanceof RegistryAccess registryAccess) {
      return getResultItem(registryAccess);
    }
    return ItemStack.EMPTY;
  }

  /** Transitional bridge for recipes still using the pre-1.21 registry access signature. */
  @Deprecated
  default ItemStack getResultItem(RegistryAccess access) {
    return ItemStack.EMPTY;
  }

  /** Gets the ingredients for display, removed from {@link Recipe} in 1.21.2 */
  default NonNullList<Ingredient> getIngredients() {
    return NonNullList.create();
  }

  /** Gets the item for recipe toasts, removed from {@link Recipe} in 1.21.2 */
  default ItemStack getToastSymbol() {
    return ItemStack.EMPTY;
  }

  /** @deprecated Means nothing outside of crafting tables */
  @Deprecated
  default boolean canCraftInDimensions(int width, int height) {
    return true;
  }

  /**
   * Returns true to hide this recipe from the recipe book. Needed until Forge has proper recipe book support.
   * @return  True
   */
  @Override
  default boolean isSpecial() {
    return true;
  }

  /** Our recipes are not placed by the recipe book */
  @Override
  default PlacementInfo placementInfo() {
    return PlacementInfo.NOT_PLACEABLE;
  }

  /** Our recipes do not show in the recipe book, so any category works */
  @Override
  default RecipeBookCategory recipeBookCategory() {
    return RecipeBookCategories.CRAFTING_MISC;
  }
}
