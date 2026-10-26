package slimeknights.mantle.recipe.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.IMultiRecipe;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helpers used in creation of recipes
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeHelper {

  /* Recipe manager utils */

  /**
   * Gets a recipe of a specific class type by name from the manager
   * @param manager  Recipe manager
   * @param name     Recipe name
   * @param clazz    Output class
   * @param <C>      Return type
   * @return  Optional of the recipe, or empty if the recipe is missing
   */
  public static <C extends Recipe<?>> Optional<C> getRecipe(RecipeManager manager, Identifier name, Class<C> clazz) {
    return getRecipe(manager.recipeMap(), name, clazz);
  }

  /** Gets a recipe of a specific class type by name, works on both sides, see {@link RecipeAccessHelper} */
  public static <C extends Recipe<?>> Optional<C> getRecipe(Level level, Identifier name, Class<C> clazz) {
    return getRecipe(RecipeAccessHelper.getRecipeMap(level), name, clazz);
  }

  /** Gets a recipe of a specific class type by name */
  public static <C extends Recipe<?>> Optional<C> getRecipe(RecipeMap recipes, Identifier name, Class<C> clazz) {
    return Optional.ofNullable(recipes.byKey(ResourceKey.create(Registries.RECIPE, name))).map(holder -> holder.value()).filter(clazz::isInstance).map(clazz::cast);
  }

  /**
   * Gets a list of all recipes from the manager, safely casting to the specified type. Multi Recipes are kept as a single recipe instance
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Preferred recipe class type
   * @param <I>  Inventory interface type
   * @param <T>  Recipe class
   * @param <C>  Return type
   * @return  List of recipes from the manager
   */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
    return getRecipes(manager.recipeMap(), type, clazz);
  }

  /** Gets a list of all recipes, works on both sides, see {@link RecipeAccessHelper} */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getRecipes(Level level, RecipeType<T> type, Class<C> clazz) {
    return getRecipes(RecipeAccessHelper.getRecipeMap(level), type, clazz);
  }

  /** Gets a list of all recipes, safely casting to the specified type */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getRecipes(RecipeMap recipes, RecipeType<T> type, Class<C> clazz) {
    return recipes.byType(type).stream().map(holder -> holder.value())
                  .filter(clazz::isInstance)
                  .map(clazz::cast)
                  .collect(Collectors.toList());
  }

  /**
   * Gets a list of recipes for display in a UI list, such as UI buttons. Will be sorted to keep the order the same on both sides, and filtered based on the given predicate and class
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Preferred recipe class type
   * @param filter   Filter for which recipes to add to the list
   * @param <I>  Inventory interface type
   * @param <T>  Recipe class
   * @param <C>  Return type
   * @return  Recipe list
   */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getUIRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz, Predicate<? super C> filter) {
    return getUIRecipes(manager.recipeMap(), type, clazz, filter);
  }

  /** Gets a list of recipes for display in a UI list, works on both sides, see {@link RecipeAccessHelper} */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getUIRecipes(Level level, RecipeType<T> type, Class<C> clazz, Predicate<? super C> filter) {
    return getUIRecipes(RecipeAccessHelper.getRecipeMap(level), type, clazz, filter);
  }

  /** Gets a list of recipes for display in a UI list, sorted to keep both sides in the same order */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getUIRecipes(RecipeMap recipes, RecipeType<T> type, Class<C> clazz, Predicate<? super C> filter) {
    return recipes.byType(type).stream()
                  .sorted(Comparator.comparing(holder -> holder.id()))
                  .map(holder -> holder.value())
                  .filter(clazz::isInstance)
                  .map(clazz::cast)
                  .filter(filter)
                  .collect(Collectors.toList());
  }

  /**
   * Gets a list of all recipes from the manager, expanding multi recipes. Intended for use in recipe display such as JEI
   * @param <C>  Return type
   * @param access   Registry access instance
   * @param recipes  Stream of recipes
   * @param clazz    Preferred recipe class type
   * @return  List of flattened recipes from the manager
   */
  public static <C> List<C> getJEIRecipes(RegistryAccess access, Stream<? extends Recipe<?>> recipes, Class<C> clazz) {
    return recipes
        .sorted((r1, r2) -> {
          // if one is multi, and the other not, the multi recipe is larger
          boolean m1 = r1 instanceof IMultiRecipe<?>;
          boolean m2 = r2 instanceof IMultiRecipe<?>;
          if (m1 && !m2) return 1;
          if (!m1 && m2) return -1;
          // Codec-backed recipes no longer own their IDs in 1.21; class name is stable for display.
          return r1.getClass().getName().compareTo(r2.getClass().getName());
        })
        .flatMap((recipe) -> {
          // if its a multi recipe, extract child recipes and stream those
          if (recipe instanceof IMultiRecipe<?>) {
            // most multiregistries iterate some external registry to list their contents
            // sometimes people do dumb things and register broken objects, so best to avoid breaking the rest of the JEI plugin
            try {
              return ((IMultiRecipe<?>) recipe).getRecipes(access).stream();
            } catch (Exception e) {
              Mantle.logger.error("Failed to fetch JEI recipes for multi recipe {}", recipe, e);
              return Stream.empty();
            }
          }
          return Stream.of(recipe);
        })
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .collect(Collectors.toList());
  }

  /**
   * Gets a list of all recipes from the manager, expanding multi recipes. Intended for use in recipe display such as JEI
   * @param <C>  Return type
   * @param access   Registry access instance
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Preferred recipe class type
   * @return  List of flattened recipes from the manager
   */
  public static <I extends RecipeInput, T extends Recipe<I>, C> List<C> getJEIRecipes(RegistryAccess access, RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
    return getJEIRecipes(access, manager.recipeMap(), type, clazz);
  }

  /** Gets a list of all recipes, expanding multi recipes. Intended for use in recipe display such as JEI */
  public static <I extends RecipeInput, T extends Recipe<I>, C> List<C> getJEIRecipes(RegistryAccess access, RecipeMap recipes, RecipeType<T> type, Class<C> clazz) {
    return getJEIRecipes(access, recipes.byType(type).stream().map(holder -> holder.value()), clazz);
  }
}
