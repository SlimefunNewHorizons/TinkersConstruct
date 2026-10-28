package slimeknights.tconstruct.library.client.model.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.DynamicItemModels;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Model for an item with material texture variants, such as tool parts. Used only for single material items, {@link ToolModel} is used for multi-material items.
 * The model bakes the material from the model JSON, or the unknown material, {@link MaterialModel.Unbaked} swaps in the material from the stack.
 */
@Getter
public class MaterialModel extends AbstractUnbakedModel {
  /** Shared loader instance */
  public static final UnbakedModelLoader<MaterialModel> LOADER = MaterialModel::deserialize;
  /** Name of the texture in the model */
  private static final String TEXTURE = "texture";

  /** If null, uses dynamic material */
  @Nullable
  private final MaterialVariantId material;
  /** Tint index and index of part in tool */
  private final int index;
  /** Transform matrix to apply to child parts */
  private final Vec2 offset;

  public MaterialModel(StandardModelParameters parameters, @Nullable MaterialVariantId material, int index, Vec2 offset) {
    super(parameters);
    this.material = material;
    this.index = index;
    this.offset = offset;
  }

  /** Gets the transform for the offset */
  public Transformation getTransforms() {
    if (Vec2.ZERO.equals(offset)) {
      return Transformation.identity();
    }
    // divide by 16 to convert from pixels to base values
    // negate Y as positive is up for transforms but down for pixels
    return new Transformation(new Vector3f(offset.x / 16, -offset.y / 16, 0), null, null, null);
  }

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) this::bake;
  }

  /** Bakes the model with its static material */
  private QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName, ContextMap additionalProperties) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    Material texture = getTexture(textureSlots);
    getQuadsForMaterial(material -> baker.sprites().get(material, debugName), texture, Objects.requireNonNullElse(material, IMaterial.UNKNOWN_ID), index, getTransforms(), null)
      .forEach(builder::addUnculledFace);
    return builder.build();
  }

  /** Gets the material texture */
  private static Material getTexture(TextureSlots textureSlots) {
    return Objects.requireNonNullElse(textureSlots.getMaterial(TEXTURE), new Material(ModelManager.BLOCK_OR_ITEM, MissingTextureAtlasSprite.getLocation()));
  }

  /**
   * Checks that all unique material textures for the given part exist, logs any that are missing via the sprite getter function.
   * @param texture       Texture to check
   * @param spriteGetter  Sprite getter, logging missing sprites
   * @param material      Material to check, null checks all
   */
  public static void validateMaterialTextures(Material texture, Function<Material, TextureAtlasSprite> spriteGetter, @Nullable MaterialVariantId material) {
    // if the texture is missing, stop here with a warning for the root
    if (!MissingTextureAtlasSprite.getLocation().equals(texture.texture())) {
      // if no specific material is set, load all materials as dependencies. If just one material, use just that one
      if (material == null) {
        MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().forEach(info -> info.getSprite(texture, spriteGetter));
      } else {
        MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material).ifPresent(info -> info.getSprite(texture, spriteGetter));
      }
    }
  }

  /**
   * Gets the tinted sprite info for the given material
   * @param spriteGetter  Sprite getter instance
   * @param texture       Base texture
   * @param material      Material variant
   * @return  Tinted sprite or fallback
   */
  @SuppressWarnings("OptionalIsPresent")
  public static TintedSprite getMaterialSprite(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material) {
    // if the base material is non-null, try to find the sprite for that material
    // first, find a render info
    Optional<MaterialRenderInfo> optional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    if (optional.isPresent()) {
      return optional.get().getSprite(texture, spriteGetter);
    }
    return new TintedSprite(spriteGetter.apply(texture), -1, 0);
  }

  /**
   * Gets quads for the given material variant of the texture
   * @param spriteGetter    Sprite getter instance
   * @param texture         Base texture
   * @param material        Material variant
   * @param tintIndex       Tint index for quads
   * @param transformation  Transformation to apply
   * @param pixels          Pixels to prevent z-fighting for multiple layers
   * @return  Quad list
   */
  public static List<BakedQuad> getQuadsForMaterial(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material, int tintIndex, Transformation transformation, @Nullable ItemLayerPixels pixels) {
    TintedSprite sprite = getMaterialSprite(spriteGetter, texture, material);
    return MantleItemLayerModel.getQuadsForSprite(sprite.color(), tintIndex, sprite.sprite(), transformation, sprite.emissivity(), pixels);
  }


  /* Item model */

  /** Item model swapping in the material from the stack, as item models moved to item definitions in 1.21.4 */
  public record Unbaked(Identifier model) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)
    ).apply(inst, Unbaked::new));

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context) {
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model);
      TextureSlots textureSlots = resolved.getTopTextureSlots();
      ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, textureSlots);
      MaterialModel materialModel = DynamicItemModels.find(resolved, MaterialModel.class);
      List<BakedQuad> defaultQuads = resolved.bakeTopGeometry(textureSlots, baker, net.minecraft.client.resources.model.BlockModelRotation.IDENTITY).getAll();
      if (materialModel == null || materialModel.material != null) {
        return new Baked(null, getTexture(textureSlots), 0, Transformation.identity(), properties, defaultQuads);
      }
      return new Baked(materialModel, getTexture(textureSlots), materialModel.index, materialModel.getTransforms(), properties, defaultQuads);
    }
  }

  /** Baked item model, caching the quads for each material */
  private record Baked(@Nullable MaterialModel model, Material texture, int index, Transformation transforms, ModelRenderProperties properties, List<BakedQuad> defaultQuads, Map<MaterialVariantId,List<BakedQuad>> cache) implements ItemModel {
    private Baked(@Nullable MaterialModel model, Material texture, int index, Transformation transforms, ModelRenderProperties properties, List<BakedQuad> defaultQuads) {
      this(model, texture, index, transforms, properties, defaultQuads, new ConcurrentHashMap<>());
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      List<BakedQuad> quads = defaultQuads;
      if (model != null) {
        // fetch the material from the stack, caching all baked quads as materials will not change
        MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
        quads = cache.computeIfAbsent(material, mat -> getQuadsForMaterial(ModelHelper::getSprite, texture, mat, index, transforms, null));
      }
      DynamicItemModels.addQuads(state, stack, quads, properties, context, new int[0]);
    }
  }


  /* Helpers */

  /** Loads a material model from JSON */
  public static MaterialModel deserialize(JsonObject json, JsonDeserializationContext context) {
    // need tint index for tool models, doubles as part index
    int index = GsonHelper.getAsInt(json, "index", 0);

    // static material can be defined, if unset uses dynamic material
    MaterialVariantId material = null;
    if (json.has("material")) {
      material = MaterialVariantId.fromJson(json, "material");
    }

    Vec2 offset = Vec2.ZERO;
    if (json.has("offset")) {
      offset = getVec2(json, "offset");
    }

    return new MaterialModel(StandardModelParameters.parse(json, context), material, index, offset);
  }

  /**
   * Converts a JSON float array to the specified object
   * @param json    JSON object
   * @param name    Name of the array in the object to fetch
   * @return  Vector3f of data
   * @throws JsonParseException  If there is no array or the length is wrong
   */
  public static Vec2 getVec2(JsonObject json, String name) {
    JsonArray array = GsonHelper.getAsJsonArray(json, name);
    if (array.size() != 2) {
      throw new JsonParseException("Expected " + 2 + " " + name + " values, found: " + array.size());
    }
    float[] vec = new float[2];
    for(int i = 0; i < 2; ++i) {
      vec[i] = GsonHelper.convertToFloat(array.get(i), name + "[" + i + "]");
    }
    return new Vec2(vec[0], vec[1]);
  }
}
