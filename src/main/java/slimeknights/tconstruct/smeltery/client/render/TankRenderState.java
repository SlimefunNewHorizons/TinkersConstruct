package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** Render state for blocks showing a tank and items, copied from the block entity when extracting */
public class TankRenderState extends BlockEntityRenderState {
  public List<FluidCuboid> fluids = List.of();
  public FluidStack fluid = FluidStack.EMPTY;
  public int capacity;
  public float offset;
  public List<RenderItem> renderItems = List.of();
  public List<ItemStackRenderState> items = List.of();
  /** Direction to rotate the contents, null for no rotation */
  @Nullable
  public Direction facing;

  /** Copies the tank contents, updating its animation */
  public void extractTank(FluidTankAnimated tank, List<FluidCuboid> fluids, float partialTicks) {
    this.fluids = fluids;
    this.fluid = tank.getFluid().copy();
    this.capacity = tank.getCapacity();
    this.offset = RenderUtils.updateRenderOffset(tank, partialTicks);
  }

  /** Resolves the items shown by the block state */
  public void extractItems(ItemModelResolver resolver, IntFunction<ItemStack> stacks, @Nullable Level level) {
    renderItems = RenderItem.STATE_REGISTRY.get(blockState, List.of());
    List<ItemStackRenderState> items = new ArrayList<>(renderItems.size());
    int seed = (int) blockPos.asLong();
    for (int i = 0; i < renderItems.size(); i++) {
      items.add(RenderingHelper.extractItem(resolver, stacks.apply(i), renderItems.get(i), level, seed + i));
    }
    this.items = items;
  }

  /** Submits the tank fluids scaled to the tank fill */
  public void submitFluids(PoseStack matrices, SubmitNodeCollector collector, boolean flipGas) {
    if (!fluids.isEmpty() && !fluid.isEmpty() && capacity > 0) {
      List<FluidCuboid> fluids = this.fluids;
      FluidStack fluid = this.fluid;
      int capacity = this.capacity;
      float offset = this.offset;
      int light = this.lightCoords;
      FluidRenderer.submit(collector, matrices, (pose, buffer) -> {
        for (FluidCuboid cube : fluids) {
          RenderUtils.renderFluidTank(pose, buffer, cube, fluid, capacity, offset, light, flipGas);
        }
      });
    }
  }

  /** Submits the items */
  public void submitItems(PoseStack matrices, SubmitNodeCollector collector) {
    for (int i = 0; i < renderItems.size(); i++) {
      RenderingHelper.renderItem(matrices, collector, items.get(i), renderItems.get(i), lightCoords);
    }
  }

  /** Submits fluids and items, rotated by {@link #facing} */
  public void submit(PoseStack matrices, SubmitNodeCollector collector, boolean flipGas) {
    boolean isRotated = facing != null && RenderingHelper.applyRotation(matrices, facing);
    submitFluids(matrices, collector, flipGas);
    submitItems(matrices, collector);
    if (isRotated) {
      matrices.popPose();
    }
  }
}
