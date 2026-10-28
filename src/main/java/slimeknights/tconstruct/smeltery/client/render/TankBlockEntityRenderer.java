package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class TankBlockEntityRenderer<T extends BlockEntity & ITankBlockEntity> implements BlockEntityRenderer<T,TankRenderState> {
  public TankBlockEntityRenderer(Context context) {}

  @Override
  public TankRenderState createRenderState() {
    return new TankRenderState();
  }

  @Override
  public void extractRenderState(T tile, TankRenderState state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, camera, breakProgress);
    state.extractTank(tile.getTank(), Config.CLIENT.tankFluidModel.get() ? List.of() : FluidCuboid.REGISTRY.get(state.blockState, List.of()), partialTicks);
  }

  @Override
  public void submit(TankRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    state.submitFluids(matrices, collector, true);
  }
}
