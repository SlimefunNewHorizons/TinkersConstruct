package slimeknights.mantle.data.loadable.common;

import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;
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

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    // vanilla ingredients cannot be empty since 1.21.2, keep supporting the legacy empty array
    if (element.isJsonArray() && element.getAsJsonArray().isEmpty()) {
      if (this == DISALLOW_EMPTY) {
        throw new JsonSyntaxException("Ingredient cannot be empty");
      }
      return IngredientHelper.EMPTY;
    }
    Ingredient ingredient = Ingredient.CODEC.parse(RegistryOpsScope.get(), element).getOrThrow(JsonParseException::new);
    // only the empty marker can be checked here, tags are not bound while datapacks load
    if (this == DISALLOW_EMPTY && ingredient.getCustomIngredient() == EmptyIngredient.INSTANCE) {
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
    return Ingredient.CODEC.encodeStart(RegistryOpsScope.get(), object).getOrThrow(JsonParseException::new);
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
