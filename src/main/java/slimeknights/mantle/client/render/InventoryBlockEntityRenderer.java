package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class InventoryBlockEntityRenderer<T extends BlockEntity & Container> implements BlockEntityRenderer<T, InventoryBlockEntityRenderer.State> {
  private final ItemModelResolver itemModelResolver;

  public InventoryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  /** Render state holding the items to display */
  public static class State extends BlockEntityRenderState {
    public List<RenderItem> renderItems = List.of();
    public List<ItemStackRenderState> items = List.of();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T inventory, State state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(inventory, state, partialTicks, camera, breakProgress);
    // find the model for item display locations
    state.renderItems = inventory.isEmpty() ? List.of() : RenderItem.STATE_REGISTRY.get(state.blockState, List.of());
    int seed = (int) inventory.getBlockPos().asLong();
    List<ItemStackRenderState> items = new ArrayList<>(state.renderItems.size());
    for (int i = 0; i < state.renderItems.size(); i++) {
      items.add(RenderingHelper.extractItem(itemModelResolver, inventory.getItem(i), state.renderItems.get(i), inventory.getLevel(), seed + i));
    }
    state.items = items;
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.renderItems.isEmpty()) return;

    // if the block is rotatable, rotate item display
    boolean isRotated = RenderingHelper.applyRotation(matrices, state.blockState);

    // render items
    for (int i = 0; i < state.renderItems.size(); i++) {
      RenderingHelper.renderItem(matrices, collector, state.items.get(i), state.renderItems.get(i), state.lightCoords);
    }

    // pop back rotation
    if (isRotated) {
      matrices.popPose();
    }
  }

  @Override
  public boolean shouldRenderOffScreen() {
    // items may stick out of the block, the old check skipped this for empty inventories
    return true;
  }
}
