package slimeknights.mantle.data.loadable.common;

import slimeknights.mantle.recipe.helper.IngredientHelper;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Loadable for ingredients, handling Forge ingredients */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY,
  DISALLOW_EMPTY;

  private static final RegistryOps<JsonElement> JSON_OPS = RegistryOps.create(
    JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    // vanilla ingredients cannot be empty since 1.21.2, keep supporting the legacy empty array
    if (element.isJsonArray() && element.getAsJsonArray().isEmpty()) {
      if (this == DISALLOW_EMPTY) {
        throw new JsonSyntaxException("Ingredient cannot be empty");
      }
      return IngredientHelper.EMPTY;
    }
    Ingredient ingredient = Ingredient.CODEC.parse(JSON_OPS, element).getOrThrow(JsonParseException::new);
    if (this == DISALLOW_EMPTY && IngredientHelper.isEmpty(ingredient)) {
      throw new JsonSyntaxException("Ingredient cannot be empty");
    }
    return ingredient;
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    if (IngredientHelper.isEmpty(object)) {
      if (this == DISALLOW_EMPTY) {
        throw new IllegalArgumentException("Ingredient cannot be empty");
      }
      return new JsonArray();
    }
    return Ingredient.CODEC.encodeStart(JSON_OPS, object).getOrThrow(JsonParseException::new);
  }

  @Override
  public Ingredient decode(FriendlyByteBuf buffer, TypedMap context) {
    return Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Ingredient object) {
    Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, object);
  }
}
