package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity,FaucetBlockEntityRenderer.State> {
  public FaucetBlockEntityRenderer(Context context) {}

  /** Faucet state */
  public static class State extends BlockEntityRenderState {
    FluidStack fluid = FluidStack.EMPTY;
    List<FluidCuboid> fluids = List.of();
    @Nullable
    Level level;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(FaucetBlockEntity faucet, State state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(faucet, state, partialTicks, camera, breakProgress);
    FluidStack renderFluid = faucet.getRenderFluid();
    state.level = faucet.getLevel();
    // fetch faucet model to determine where to render fluids
    state.fluids = faucet.isPouring() && !renderFluid.isEmpty() ? FluidCuboid.REGISTRY.get(state.blockState, List.of()) : List.of();
    state.fluid = renderFluid.copy();
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    Level world = state.level;
    if (state.fluids.isEmpty() || world == null) {
      return;
    }
    // if side, rotate fluid model
    Direction direction = state.blockState.getValue(FaucetBlock.FACING);
    boolean isRotated = RenderingHelper.applyRotation(matrices, direction);

    // fluid props
    FluidStack renderFluid = state.fluid;
    IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(renderFluid.getFluid());
    int color = attributes.getTintColor(renderFluid);
    TextureAtlasSprite still = FluidRenderer.getBlockSprite(attributes.getStillTexture(renderFluid));
    TextureAtlasSprite flowing = FluidRenderer.getBlockSprite(attributes.getFlowingTexture(renderFluid));
    int light = FluidRenderer.withBlockLight(state.lightCoords, renderFluid.getFluid().getFluidType().getLightLevel(renderFluid));
    List<FluidCuboid> fluids = state.fluids;
    BlockPos pos = state.blockPos;

    FluidRenderer.submit(collector, matrices, (pose, buffer) -> {
      // render all cubes in the model
      for (FluidCuboid cube : fluids) {
        FluidRenderer.renderCuboid(pose, buffer, cube, 0, still, flowing, color, light, false);
      }
      // render into the block(s) below
      RenderingHelper.renderFaucetFluids(world, pos, direction, pose, buffer, still, flowing, color, light);
    });

    // if rotated, pop back rotation
    if (isRotated) {
      matrices.popPose();
    }
  }
}
