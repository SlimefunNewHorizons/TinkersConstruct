package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelHelper {
  private static final Map<Block,Identifier> TEXTURE_NAME_CACHE = new ConcurrentHashMap<>();
  /** Listener instance to clear cache */
  public static final ResourceManagerReloadListener LISTENER = manager -> TEXTURE_NAME_CACHE.clear();

  /**
   * Gets the texture name for a block from the model manager
   * @param block  Block to fetch
   * @return Texture name for the block
   */
  private static Identifier getParticleTextureInternal(Block block) {
    TextureAtlasSprite particle = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).particleIcon();
    //noinspection ConstantConditions  dumb mods returning null particle icons
    if (particle != null) {
      return particle.contents().name();
    }
    return MissingTextureAtlasSprite.getLocation();
  }

  /**
   * Gets the name of a particle texture for a block, using the cached value if present
   * @param block Block to fetch
   * @return Texture name for the block
   */
  public static Identifier getParticleTexture(Block block) {
    return TEXTURE_NAME_CACHE.computeIfAbsent(block, ModelHelper::getParticleTextureInternal);
  }

  /** Validates the given rotation is in 90 degree increments */
  public static boolean checkRotation(float rotation) {
    return rotation >= 0 && rotation % 90 == 0 && rotation <= 270;
  }

  /**
   * Gets a rotation from JSON
   * @param json  JSON parent
   * @return  Integer of 0, 90, 180, or 270
   */
  public static int getRotation(JsonObject json, String key) {
    int i = GsonHelper.getAsInt(json, key, 0);
    if (checkRotation(i)) {
      return i;
    } else {
      throw new JsonParseException("Invalid '" + key + "' " + i + " found, only 0/90/180/270 allowed");
    }
  }

  /* Loading */

  /**
   * Parses the vanilla part of a model with a custom loader, so custom loaders only need to handle their own keys
   * @param json     Model JSON, not modified
   * @param context  Deserialization context
   * @return  Vanilla model
   */
  public static UnbakedModel parseVanilla(JsonObject json, JsonDeserializationContext context) {
    JsonObject copy = json.deepCopy();
    copy.remove("loader");
    return context.deserialize(copy, UnbakedModel.class);
  }

  /**
   * Gets the elements to bake for a model, models without elements use the elements of their parents
   * @param geometry  Geometry of the model, null if it has no elements
   * @param parent    Parent of the model
   * @param baker     Baker to resolve the parent
   * @return  Elements of the model or its closest parent with elements
   */
  public static List<BlockElement> getElements(@Nullable UnbakedGeometry geometry, @Nullable Identifier parent, ModelBaker baker) {
    if (geometry == null && parent != null) {
      geometry = baker.getModel(parent).getTopGeometry();
    }
    if (geometry instanceof SimpleUnbakedGeometry simple) {
      return simple.elements();
    }
    return List.of();
  }
}
