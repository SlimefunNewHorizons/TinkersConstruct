package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.tconstruct.smeltery.block.entity.GaugeBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class GaugeBlockEntityRenderer implements BlockEntityRenderer<GaugeBlockEntity,TankRenderState> {
  public GaugeBlockEntityRenderer(Context context) {}

  @Override
  public TankRenderState createRenderState() {
    return new TankRenderState();
  }

  @Override
  public void extractRenderState(GaugeBlockEntity tile, TankRenderState state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, camera, breakProgress);
    state.fluids = FluidCuboid.REGISTRY.get(state.blockState, List.of());
    IFluidHandler tank = tile.getTank();
    state.fluid = !state.fluids.isEmpty() && tank.getTanks() > 0 ? tank.getFluidInTank(0).copy() : FluidStack.EMPTY;
  }

  @Override
  public void submit(TankRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    if (!state.fluid.isEmpty()) {
      List<FluidCuboid> fluids = state.fluids;
      FluidStack fluid = state.fluid;
      int light = state.lightCoords;
      FluidRenderer.submit(collector, matrices, (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, fluids, fluid, light));
    }
  }
}
