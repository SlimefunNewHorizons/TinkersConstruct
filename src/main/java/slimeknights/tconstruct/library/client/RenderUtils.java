package slimeknights.tconstruct.library.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RenderUtils {
  /**
   * Adds a fluid cuboid with transparency
   * @param matrices  Matrix stack instance
   * @param buffer    Fluid buffer, see {@link FluidRenderer#submit}
   * @param fluid     Fluid to render
   * @param opacity   Fluid opacity to blend in
   * @param light     Quad lighting
   * @param cube      Fluid cuboid instance
   */
  public static void renderTransparentCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, FluidStack fluid, int opacity, int light) {
    // nothing to render? skip
    if (opacity < 0 || fluid.isEmpty()) {
      return;
    }

    IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid.getFluid());
    TextureAtlasSprite still = FluidRenderer.getBlockSprite(attributes.getStillTexture(fluid));
    TextureAtlasSprite flowing = FluidRenderer.getBlockSprite(attributes.getFlowingTexture(fluid));
    FluidType fluidType = fluid.getFluid().getFluidType();
    boolean isGas = fluidType.isLighterThanAir();
    light = FluidRenderer.withBlockLight(light, fluidType.getLightLevel(fluid));

    // add in fluid opacity if given
    int color = attributes.getTintColor(fluid);
    if (opacity < 0xFF) {
      // alpha is top 8 bits, multiply by opacity and divide out remainder
      int alpha = ((color >> 24) & 0xFF) * opacity / 0xFF;
      // clear bits in color and or in the new alpha
      color = (color & 0xFFFFFF) | (alpha << 24);
    }
    FluidRenderer.renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, isGas);
  }

  /**
   * Updates the animation offset of a fluid tank, call while extracting the render state
   * @param tank          Fluid tank animated to render
   * @param partialTicks  Partial ticks
   * @return  Offset to pass to {@link #renderFluidTank(PoseStack, VertexConsumer, FluidCuboid, FluidStack, int, float, int, boolean)}
   */
  public static float updateRenderOffset(FluidTankAnimated tank, float partialTicks) {
    if (tank.getFluid().isEmpty() || tank.getCapacity() <= 0) {
      // clear render offset if no liquid
      tank.setRenderOffset(0);
      return 0;
    }
    float offset = tank.getRenderOffset();
    if (offset > 1.2f || offset < -1.2f) {
      offset = offset - ((offset / 12f + 0.1f) * partialTicks);
      tank.setRenderOffset(offset);
    } else {
      tank.setRenderOffset(0);
    }
    return offset;
  }

  /**
   * Add textured quads for a fluid tank
   * @param matrices  Matrix stack instance
   * @param buffer    Fluid buffer, see {@link FluidRenderer#submit}
   * @param cube      Fluid cuboid instance
   * @param liquid    Fluid in the tank
   * @param capacity  Tank capacity
   * @param offset    Animation offset from {@link #updateRenderOffset(FluidTankAnimated, float)}
   * @param light     Quad lighting
   * @param flipGas   If true, flips gas cubes
   */
  public static void renderFluidTank(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, FluidStack liquid, int capacity, float offset, int light, boolean flipGas) {
    if (!liquid.isEmpty() && capacity > 0) {
      FluidRenderer.renderScaledCuboid(matrices, buffer, cube, liquid, offset, capacity, light, flipGas);
    }
  }
}
