"""Ports MantleItemLayerModel to the 1.21.11 model API, keeping the quad generation"""
from portio import read, write

P = 'src/main/java/slimeknights/mantle/client/model/util/MantleItemLayerModel.java'
text = read(P)
nl = '\r\n' if '\r\n' in text else '\n'
text = text.replace('\r\n', '\n')
start = text.index('  /**\n   * Gets all quads for an item layer for the given sprite')
end = text.index('  /**\n   * Class holding details about a single layer in the model')
body = text[start:end]
header = '''package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.neoforged.neoforge.client.model.pipeline.TransformingVertexPipeline;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.mantle.util.ReversedListBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.PrimitiveIterator;

/**
 * Item layer model with a hardcoded color per layer, allows reducing rendering time by bypassing item colors for a static color.
 * Also supports luminosity, and when used as a model loader supports telling a layer to not use a tint index
 */
public class MantleItemLayerModel extends AbstractUnbakedModel {
  /** Model loader instance */
  public static final UnbakedModelLoader<MantleItemLayerModel> LOADER = MantleItemLayerModel::deserialize;

  private static final Direction[] HORIZONTALS = {Direction.UP, Direction.DOWN};
  private static final Direction[] VERTICALS = {Direction.WEST, Direction.EAST};

  /** Layers in the model */
  private final List<LayerData> layers;

  public MantleItemLayerModel(StandardModelParameters parameters, List<LayerData> layers) {
    super(parameters);
    this.layers = layers;
  }

  /** Gets the layer at the given index */
  private LayerData getLayer(int index) {
    return LogicHelper.getOrDefault(layers, index, LayerData.DEFAULT);
  }

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) this::bake;
  }

  /**
   * Applies the root transform of the model to the model state.
   */
  public static ModelState applyTransform(ModelState modelState, ContextMap additionalProperties) {
    Transformation transformation = additionalProperties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity());
    return transformation.isIdentity() ? modelState : UnbakedElementsHelper.composeRootTransformIntoModelState(modelState, transformation);
  }

  /** Bakes all layers of the model */
  private QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName, ContextMap additionalProperties) {
    List<Material> textures = new ArrayList<>();
    for (int i = 0; textureSlots.getMaterial("layer" + i) != null; i++) {
      textures.add(textureSlots.getMaterial("layer" + i));
    }
    // skip the pixel tracking if using a single texture only
    ItemLayerPixels pixels = textures.size() == 1 ? null : new ItemLayerPixels();
    Transformation transform = applyTransform(modelState, additionalProperties).transformation();
    // layers are built top down so lower layers skip covered pixels, but must be added bottom up
    // shortcut: per layer render types are ignored, the model render type applies to all layers
    ReversedListBuilder<List<BakedQuad>> quads = new ReversedListBuilder<>();
    for (int i = textures.size() - 1; i >= 0; i--) {
      TextureAtlasSprite sprite = baker.sprites().get(textures.get(i), debugName);
      LayerData data = getLayer(i);
      quads.add(getQuadsForSprite(data.color(), data.noTint() ? -1 : i, sprite, transform, data.luminosity(), pixels));
    }
    QuadCollection.Builder builder = new QuadCollection.Builder();
    quads.build(list -> list.forEach(builder::addUnculledFace));
    return builder.build();
  }

'''


def sub(old, new, count=-1):
  global body
  assert old in body, old
  body = body.replace(old, new, count)


# the quad builder clears sprite and emission after each quad, so set them per quad
sub('''    // common settings
    quadBuilder.setSprite(sprite);
    quadBuilder.setTintIndex(tint);''', '''    // common settings, sprite and emission are set per quad as baking clears them
    quadBuilder.setTintIndex(tint);''')
sub('builder.add(buildQuad(quadBuilder, quadConsumer, Direction.NORTH, color, emissivity,', 'builder.add(buildQuad(quadBuilder, quadConsumer, Direction.NORTH, sprite, color, emissivity,')
sub('builder.add(buildQuad(quadBuilder, quadConsumer, Direction.SOUTH, color, emissivity,', 'builder.add(buildQuad(quadBuilder, quadConsumer, Direction.SOUTH, sprite, color, emissivity,')
sub('    return buildQuad(quadBuilder, quadConsumer, Direction.SOUTH, color, emissivity,', '    return buildQuad(quadBuilder, quadConsumer, Direction.SOUTH, sprite, color, emissivity,')
sub('''    return buildQuad(builder, consumer, (side.getAxis() == Axis.Y ? side.getOpposite() : side),
      color, luminosity,''', '''    return buildQuad(builder, consumer, (side.getAxis() == Axis.Y ? side.getOpposite() : side),
      sprite, color, luminosity,''')
sub('public static BakedQuad buildQuad(QuadBakingVertexConsumer builder, VertexConsumer consumer, Direction side, int color, int luminosity,',
    'public static BakedQuad buildQuad(QuadBakingVertexConsumer builder, VertexConsumer consumer, Direction side, TextureAtlasSprite sprite, int color, int luminosity,')
sub('''    builder.setDirection(side);
    putVertex(''', '''    builder.setDirection(side);
    builder.setSprite(sprite);
    builder.setLightEmission(luminosity);
    putVertex(''', 1)
sub('''   * @param side         Quad side
   * @param color''', '''   * @param side         Quad side
   * @param sprite       Quad sprite
   * @param color''')
body = body.replace('{@link ItemLayerModel}', "NeoForge's item layer model")
body = body.replace('''    int light = (luminosity << 4);
    consumer.setUv2(light, light);
''', '')

tail = text[end:]
tail = tail.replace('''
    /** Gets the render type for this layer from the context, falling back to the passed type if not requested */
    public RenderTypeGroup getRenderType(IGeometryBakingContext context, RenderTypeGroup defaultType) {
      if (renderType == null) {
        return defaultType;
      }
      return context.getRenderType(renderType);
    }
''', '')
old = '    return new MantleItemLayerModel(LayerData.LIST_LOADABLE.getOrDefault(json, "layers", List.of()));'
assert old in tail
tail = tail.replace(old, '    return new MantleItemLayerModel(StandardModelParameters.parse(json, context), LayerData.LIST_LOADABLE.getOrDefault(json, "layers", List.of()));')
write(P, (header + body + tail).replace('\n', nl))
print('ok')
