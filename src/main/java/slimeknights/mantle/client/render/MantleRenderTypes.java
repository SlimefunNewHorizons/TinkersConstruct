package slimeknights.mantle.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Render types used by Mantle.
 * shortcut: the custom fluid and fullbright shaders are replaced by vanilla pipelines, as core shaders became render pipelines in 1.21.5.
 * Add custom pipelines if the vanilla ones look wrong.
 */
public class MantleRenderTypes {
  private MantleRenderTypes() {}

  /** Render type used for the fluid renderer, vertices need position, color, UV, light and normal */
  public static final RenderType FLUID = RenderTypes.translucentMovingBlock();

  /** Render type used for the structure renderer */
  public static final RenderType TRANSLUCENT_FULLBRIGHT = RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS);
}
