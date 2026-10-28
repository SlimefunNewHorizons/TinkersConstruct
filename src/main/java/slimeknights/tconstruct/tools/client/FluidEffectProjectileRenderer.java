package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.tconstruct.tools.entity.FluidEffectProjectile;

import java.util.List;

public class FluidEffectProjectileRenderer extends EntityRenderer<FluidEffectProjectile,FluidEffectProjectileRenderer.State> {
  private final List<FluidCuboid> fluids;
  public FluidEffectProjectileRenderer(Context context) {
    super(context);
    this.fluids = List.of(
      FluidCuboid.builder().from(-4,  0,  0).to(-2,  2,  2).build(),
      FluidCuboid.builder().from( 0, -4,  0).to( 2, -2,  2).build(),
      FluidCuboid.builder().from( 0,  0, -4).to( 2,  2, -2).build(),
      FluidCuboid.builder().from( 2,  0,  0).to( 4,  2,  2).build(),
      FluidCuboid.builder().from( 0,  0,  0).to( 2,  4,  2).build(),
      FluidCuboid.builder().from( 0,  0,  2).to( 2,  2,  4).build());
  }

  /** State for the fluid projectile */
  public static class State extends EntityRenderState {
    public FluidStack fluid = FluidStack.EMPTY;
    public float yRot;
    public float xRot;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(FluidEffectProjectile entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.fluid = entity.getFluid().copy();
    state.yRot = entity.getYRot(partialTicks);
    state.xRot = entity.getXRot(partialTicks);
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    matrices.pushPose();
    matrices.translate(0.0D, 0.15F, 0.0D);
    matrices.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
    matrices.mulPose(Axis.ZP.rotationDegrees(state.xRot));
    FluidStack fluid = state.fluid;
    int light = state.lightCoords;
    FluidRenderer.submit(collector, matrices, (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, fluids, fluid, light));
    matrices.popPose();
    super.submit(state, matrices, collector, camera);
  }
}
