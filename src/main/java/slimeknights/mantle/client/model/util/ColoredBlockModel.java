package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.DelegateUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import org.apache.commons.lang3.mutable.MutableObject;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.LogicHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Model applying a static color and light to each element, elements come from the model or its parent */
public class ColoredBlockModel extends DelegateUnbakedModel {
  /** Model loader to allow doing basic coloring outside of other models */
  public static final UnbakedModelLoader<ColoredBlockModel> LOADER = ColoredBlockModel::deserialize;

  /** Colors to use for each piece */
  @Getter
  private final List<ColorData> colorData;

  public ColoredBlockModel(UnbakedModel delegate, List<ColorData> colorData) {
    super(delegate);
    this.colorData = colorData;
  }

  /** Gets the vanilla model wrapped by this model */
  public UnbakedModel getDelegate() {
    return delegate;
  }

  @Override
  public UnbakedGeometry geometry() {
    UnbakedGeometry geometry = delegate.geometry();
    Identifier parent = delegate.parent();
    return (ExtendedUnbakedGeometry) (textureSlots, baker, modelState, debugName, additionalProperties) ->
      new SimpleUnbakedGeometry(applyColors(ModelHelper.getElements(geometry, parent, baker), colorData)).bake(textureSlots, baker, modelState, debugName, additionalProperties);
  }

  /**
   * Applies colors to a list of elements.
   * shortcut: per element UV lock is ignored, UV lock is part of the block state variant since 1.21.5
   * @param elements   Elements to color
   * @param colorData  Color for each element by index
   * @return  Colored elements, elements are copied as faces link to their element
   */
  public static List<BlockElement> applyColors(List<BlockElement> elements, List<ColorData> colorData) {
    if (colorData.isEmpty()) {
      return elements;
    }
    List<BlockElement> colored = new ArrayList<>(elements.size());
    for (int i = 0; i < elements.size(); i++) {
      BlockElement element = elements.get(i);
      ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
      if (colors.color == -1 && colors.luminosity <= 0) {
        colored.add(element);
      } else {
        colored.add(withFaceData(element, new ExtraFaceData(colors.color, Math.max(colors.luminosity, element.faceData().lightEmission()), element.faceData().ambientOcclusion())));
      }
    }
    return colored;
  }

  /** Copies the element with new face data on all faces */
  public static BlockElement withFaceData(BlockElement element, ExtraFaceData data) {
    Map<Direction,BlockElementFace> faces = new EnumMap<>(Direction.class);
    element.faces().forEach((side, face) -> faces.put(side, new BlockElementFace(face.cullForDirection(), face.tintIndex(), face.texture(), face.uvs(), face.rotation(), data, new MutableObject<>())));
    return new BlockElement(element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), data);
  }

  /**
   * Data class for setting properties when baking colored elements
   */
  public record ColorData(int color, int luminosity, @Nullable Boolean uvlock) {
    public static final ColorData DEFAULT = new ColorData(-1, -1, null);
    public static final RecordLoadable<ColorData> LOADABLE = RecordLoadable.create(
      ColorLoadable.ALPHA.defaultField("color", false, ColorData::color),
      IntLoadable.range(-1, 15).defaultField("luminosity", -1, ColorData::luminosity),
      BooleanLoadable.INSTANCE.nullableField("uvlock", ColorData::uvlock),
      ColorData::new);
    public static final Loadable<List<ColorData>> LIST_LOADABLE = LOADABLE.list(0);

    /** Gets the UV lock for the given part */
    public boolean isUvLock(boolean defaultLock) {
      if (uvlock == null) {
        return defaultLock;
      }
      return uvlock;
    }
  }


  /* Deserializing */

  /** Deserializes the model from JSON */
  public static ColoredBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    return new ColoredBlockModel(ModelHelper.parseVanilla(json, context), ColorData.LIST_LOADABLE.getOrDefault(json, "colors", List.of()));
  }


  /* Colors */

  /**
   * Converts an ARGB color to an ABGR color, as the commonly used color format is not the format colors end up packed into.
   * This function doubles as its own inverse, not that its needed.
   * @param color  ARGB color
   * @return  ABGR color
   */
  public static int swapColorRedBlue(int color) {
    return (color & 0xFF00FF00) // alpha and green same spot
           | ((color >> 16) & 0x000000FF) // red moves to blue
           | ((color << 16) & 0x00FF0000); // blue moves to red
  }

  /** Copies the quad with a static ARGB color and light emission, replacing its vertex colors */
  public static BakedQuad withColorAndLight(BakedQuad quad, int color, int lightEmission) {
    return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                         quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                         quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), Math.max(quad.lightEmission(), lightEmission),
                         quad.bakedNormals(), color == -1 ? quad.bakedColors() : BakedColors.of(color), quad.hasAmbientOcclusion());
  }

  /** Copies the quad with a static ARGB color, replacing its vertex colors */
  public static BakedQuad applyColor(BakedQuad quad, int color) {
    return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                         quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                         quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), quad.lightEmission(),
                         quad.bakedNormals(), BakedColors.of(color), quad.hasAmbientOcclusion());
  }
}
