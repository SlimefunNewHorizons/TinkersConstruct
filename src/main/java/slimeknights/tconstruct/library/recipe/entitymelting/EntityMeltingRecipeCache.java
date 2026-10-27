package slimeknights.tconstruct.library.recipe.entitymelting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class handling a recipe cache for entity melting recipes, since any given entity type has one recipe
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityMeltingRecipeCache {
  private static final Map<EntityType<?>,EntityMeltingRecipe> CACHE = new HashMap<>();

  static {
    RecipeCacheInvalidator.addReloadListener(client -> CACHE.clear());
  }

  /**
   * Gets the recipe for the given type
   * @param level    Level, recipes come from the server or the client copy
   * @param type     Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  @Nullable
  public static EntityMeltingRecipe findRecipe(Level level, EntityType<?> type) {
    if (CACHE.containsKey(type)) {
      return CACHE.get(type);
    }

    // find a recipe if none exist
    for (EntityMeltingRecipe recipe : RecipeHelper.getRecipes(level, TinkerRecipeTypes.ENTITY_MELTING.get(), EntityMeltingRecipe.class)) {
      if (recipe.matches(type)) {
        CACHE.put(type, recipe);
        return recipe;
      }
    }

    // cache nothing was found
    CACHE.put(type, null);
    return null;
  }
}
