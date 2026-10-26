package slimeknights.mantle.recipe.helper;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Access to recipes from either side. Since 1.21.2 recipes only exist on the server; the server sends the types registered here
 * to clients through {@link OnDatapackSyncEvent#sendRecipes(RecipeType[])}, and the client stores them from {@code RecipesReceivedEvent}.
 */
public final class RecipeAccessHelper {
  private RecipeAccessHelper() {}

  /** Recipe types to send to clients */
  private static final Set<RecipeType<?>> SYNCED_TYPES = ConcurrentHashMap.newKeySet();
  /** Recipes received by the client */
  private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

  /** Marks the given recipe types as needed on the client */
  public static void syncToClient(RecipeType<?>... types) {
    SYNCED_TYPES.addAll(java.util.List.of(types));
  }

  /** Event listener sending the registered types to clients */
  public static void onDatapackSync(OnDatapackSyncEvent event) {
    event.sendRecipes(SYNCED_TYPES);
  }

  /** Called by the client when recipes are received */
  public static void setClientRecipes(RecipeMap recipes) {
    clientRecipes = recipes;
  }

  /** Gets all recipes available on the given side */
  public static RecipeMap getRecipeMap(@Nullable Level level) {
    if (level instanceof ServerLevel server) {
      return server.recipeAccess().recipeMap();
    }
    return clientRecipes;
  }

  /** Gets all recipes of the given type */
  public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(@Nullable Level level, RecipeType<T> type) {
    return getRecipeMap(level).byType(type);
  }

  /** Gets a recipe by ID */
  public static Optional<RecipeHolder<?>> byKey(@Nullable Level level, Identifier id) {
    return Optional.ofNullable(getRecipeMap(level).byKey(ResourceKey.create(Registries.RECIPE, id)));
  }

  /** Finds the first recipe matching the input, replacement for {@code RecipeManager#getRecipeFor} usable on both sides */
  public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(Level level, RecipeType<T> type, I input) {
    if (level instanceof ServerLevel server) {
      return server.recipeAccess().getRecipeFor(type, input, level);
    }
    return clientRecipes.getRecipesFor(type, input, level).findFirst();
  }
}
