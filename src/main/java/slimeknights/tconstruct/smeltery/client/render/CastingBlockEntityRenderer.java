package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.CastingFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

public class CastingBlockEntityRenderer implements BlockEntityRenderer<CastingBlockEntity,CastingBlockEntityRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  public CastingBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  /** Casting state, adds the fluid fade */
  public static class State extends TankRenderState {
    int fluidOpacity = 0xFF;
    boolean full;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(CastingBlockEntity casting, State state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(casting, state, partialTicks, camera, breakProgress);
    state.fluids = FluidCuboid.REGISTRY.get(state.blockState, List.of());

    // if the recipe is in progress, start fading the item away
    int timer = casting.getTimer();
    int totalTime = casting.getCoolingTime();
    int itemOpacity = 0;
    state.fluidOpacity = 0xFF;
    if (timer > 0 && totalTime > 0) {
      int opacity = (4 * 0xFF) * timer / totalTime;
      // fade item in
      itemOpacity = opacity / 4;

      // fade fluid and temperature out during last 10%
      if (opacity > 3 * 0xFF) {
        state.fluidOpacity = (4 * 0xFF) - opacity;
      }
    }

    CastingFluidHandler tank = casting.getTank();
    state.fluid = tank.getFluid().copy();
    state.capacity = tank.getCapacity();
    state.full = state.fluid.getAmount() == state.capacity;

    // output may be the recipe output instead of the current item
    // shortcut: item models cannot be tinted translucent since 1.21.4, so the recipe output pops in halfway through cooling instead of fading in
    ItemStack output = casting.getItem(1);
    if (itemOpacity > 0x7F && output.isEmpty()) {
      output = casting.getRecipeOutput();
    }
    ItemStack finalOutput = output;
    state.extractItems(itemModelResolver, i -> i == 1 ? finalOutput : casting.getItem(i), casting.getLevel());
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.fluids.isEmpty() && state.renderItems.isEmpty()) {
      return;
    }
    boolean isRotated = RenderingHelper.applyRotation(matrices, state.blockState);

    // render fluids
    if (!state.fluids.isEmpty() && !state.fluid.isEmpty()) {
      List<FluidCuboid> fluids = state.fluids;
      FluidStack fluid = state.fluid;
      int light = state.lightCoords;
      if (state.full) {
        // if full, start rendering with opacity for progress
        int opacity = state.fluidOpacity;
        FluidRenderer.submit(collector, matrices, (pose, buffer) -> {
          for (FluidCuboid cube : fluids) {
            RenderUtils.renderTransparentCuboid(pose, buffer, cube, fluid, opacity, light);
          }
        });
      } else {
        // not strictly useful to scale the fluids down, but who knows what the modeler does
        state.submitFluids(matrices, collector, false);
      }
    }

    // render items
    state.submitItems(matrices, collector);

    // pop back rotation
    if (isRotated) {
      matrices.popPose();
    }
  }
}
