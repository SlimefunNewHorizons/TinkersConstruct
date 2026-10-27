package slimeknights.mantle.client.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Item model choosing a texture based on a string in the item data.
 * shortcut: always renders the "default" texture, as item models moved to item definitions in 1.21.4.
 * Switching textures needs a select item model on the data component.
 */
@Getter
public class NBTKeyModel extends AbstractUnbakedModel {
  /** Model loader instance */
  public static final UnbakedModelLoader<NBTKeyModel> LOADER = NBTKeyModel::deserialize;

  /** Map of statically registered extra textures, used for addon mods */
  private static final Multimap<Identifier,Pair<String,Identifier>> EXTRA_TEXTURES = HashMultimap.create();

  /**
   * Registers an extra variant texture for the model with the given key. Note that resource packs can override the extra texture
   * @param key          Model key, should be defined in the model JSON if supported
   * @param textureName  Name of the texture defined, corresponds to a possible value of the NBT key
   * @param texture      Texture to use, same format as in resource packs
   */
  @SuppressWarnings("unused")  // API
  public static void registerExtraTexture(Identifier key, String textureName, Identifier texture) {
    EXTRA_TEXTURES.put(key, Pair.of(textureName, texture));
  }

  /** Gets the extra textures registered for the given key */
  public static Collection<Pair<String,Identifier>> getExtraTextures(Identifier key) {
    return EXTRA_TEXTURES.get(key);
  }

  /** Key to check in item NBT */
  private final String nbtKey;
  /** Key denoting which extra textures to fetch from the map */
  @Nullable
  private final Identifier extraTexturesKey;

  public NBTKeyModel(StandardModelParameters parameters, String nbtKey, @Nullable Identifier extraTexturesKey) {
    super(parameters);
    this.nbtKey = nbtKey;
    this.extraTexturesKey = extraTexturesKey;
  }

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) NBTKeyModel::bake;
  }

  /** Bakes the default texture as an item layer */
  private static QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName, ContextMap additionalProperties) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    Material texture = textureSlots.getMaterial("default");
    if (texture != null) {
      MantleItemLayerModel.getQuadsForSprite(-1, -1, baker.sprites().get(texture, debugName), MantleItemLayerModel.applyTransform(modelState, additionalProperties).transformation(), 0, null)
                          .forEach(builder::addUnculledFace);
    }
    return builder.build();
  }

  /** Deserializes the model from JSON */
  public static NBTKeyModel deserialize(JsonObject json, JsonDeserializationContext context) {
    String key = GsonHelper.getAsString(json, "nbt_key");
    Identifier extraTexturesKey = null;
    if (json.has("extra_textures_key")) {
      extraTexturesKey = JsonHelper.getResourceLocation(json, "extra_textures_key");
    }
    return new NBTKeyModel(StandardModelParameters.parse(json, context), key, extraTexturesKey);
  }
}
