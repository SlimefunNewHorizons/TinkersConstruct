package slimeknights.mantle.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import slimeknights.mantle.client.model.util.ColoredBlockModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Model that retextures some of its textures based on the block stored in the block entity or item.
 * shortcut: the retextured names are parsed but not applied, the model renders its default textures.
 * Applying them needs a dynamic block state model and an item model reading the texture component, see the 1.21.1 implementation in git history.
 */
@Getter
public class RetexturedModel extends ColoredBlockModel {
  public static final UnbakedModelLoader<RetexturedModel> LOADER = RetexturedModel::deserialize;

  /** Textures replaced by the block texture */
  private final Set<String> retextured;

  public RetexturedModel(ColoredBlockModel model, Set<String> retextured) {
    super(model.getDelegate(), model.getColorData());
    this.retextured = retextured;
  }

  public static RetexturedModel deserialize(JsonObject json, JsonDeserializationContext context) {
    return new RetexturedModel(ColoredBlockModel.deserialize(json, context), getRetexturedNames(json));
  }

  /**
   * Gets the list of retextured textures from the model
   * @param json  Model json
   * @return  List of textures
   */
  public static Set<String> getRetexturedNames(JsonObject json) {
    if (json.has("retextured")) {
      // if an array, set from each texture in array
      JsonElement retextured = json.get("retextured");
      if (retextured.isJsonArray()) {
        JsonArray array = retextured.getAsJsonArray();
        if (array.isEmpty()) {
          throw new JsonSyntaxException("Must have at least one texture in retextured");
        }
        List<String> builder = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
          builder.add(GsonHelper.convertToString(array.get(i), "retextured[" + i + "]"));
        }
        return Set.copyOf(builder);
      }
      // if string, single texture
      if (retextured.isJsonPrimitive()) {
        return Set.of(retextured.getAsString());
      }
    }
    // if neither or missing, error
    throw new JsonSyntaxException("Missing retextured, expected to find a String or a JsonArray");
  }
}
