package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Helpers for item models building their quads at runtime */
public final class DynamicItemModels {
  private DynamicItemModels() {}

  /**
   * Finds the closest model of the given type in the parent chain
   * @param model  Resolved model
   * @param type   Unbaked model class
   * @return  Unbaked model, or null if none in the chain
   */
  @Nullable
  public static <T extends UnbakedModel> T find(ResolvedModel model, Class<T> type) {
    for (ResolvedModel current = model; current != null; current = current.parent()) {
      if (type.isInstance(current.wrapped())) {
        return type.cast(current.wrapped());
      }
    }
    return null;
  }

  /**
   * Adds quads to the render state. Each layer renders a single atlas, so quads are split by atlas.
   * @param state       Render state
   * @param stack       Stack being rendered
   * @param quads       Quads to add, the list identity is used to cache GUI renders so it should be reused for identical stacks
   * @param properties  Transforms and particle
   * @param context     Display context
   * @param tints       Tint colors by tint index
   */
  public static void addQuads(ItemStackRenderState state, ItemStack stack, List<BakedQuad> quads, ModelRenderProperties properties, ItemDisplayContext context, int[] tints) {
    state.appendModelIdentityElement(quads);
    for (int tint : tints) {
      state.appendModelIdentityElement(tint);
    }
    Map<Identifier,List<BakedQuad>> byAtlas = new LinkedHashMap<>();
    boolean animated = false;
    for (BakedQuad quad : quads) {
      byAtlas.computeIfAbsent(quad.sprite().atlasLocation(), atlas -> new ArrayList<>()).add(quad);
      animated |= quad.sprite().contents().isAnimated();
    }
    boolean foil = stack.hasFoil();
    for (Map.Entry<Identifier,List<BakedQuad>> entry : byAtlas.entrySet()) {
      LayerRenderState layer = state.newLayer();
      int[] layerTints = layer.prepareTintLayers(tints.length);
      System.arraycopy(tints, 0, layerTints, 0, tints.length);
      layer.setRenderType(TextureAtlas.LOCATION_BLOCKS.equals(entry.getKey()) ? Sheets.translucentBlockItemSheet() : Sheets.translucentItemSheet());
      List<BakedQuad> layerQuads = entry.getValue();
      layer.setExtents(() -> BlockModelWrapper.computeExtents(layerQuads));
      properties.applyToLayer(layer, context);
      layer.prepareQuadList().addAll(layerQuads);
      if (foil) {
        layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
      }
    }
    if (animated || foil) {
      state.setAnimated();
    }
  }
}
