package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.library.client.TinkerRenderTypes;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HeatingStructureBlockEntityRenderer implements BlockEntityRenderer<HeatingStructureBlockEntity,HeatingStructureBlockEntityRenderer.State> {
  private static final float ITEM_SCALE = 15f/16f;
  /** Rough quads per item, used for the item quad limit as item render states do not expose their quads */
  private static final int QUADS_PER_ITEM = 50;

  private final ItemModelResolver itemModelResolver;
  public HeatingStructureBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  /** Item inside the structure */
  record StructureItem(ItemStackRenderState item, int x, int y, int z, int light) {}

  /** Controller state */
  public static class State extends BlockEntityRenderState {
    /** Error position relative to the controller, null to skip */
    @Nullable
    BlockPos error;
    boolean highlightError;
    boolean structureValid;
    BlockPos minPos = BlockPos.ZERO;
    BlockPos maxPos = BlockPos.ZERO;
    List<FluidStack> fluids = List.of();
    int capacity;
    int fluidLight;
    Direction facing = Direction.NORTH;
    List<StructureItem> items = List.of();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(HeatingStructureBlockEntity smeltery, State state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(smeltery, state, partialTicks, camera, breakProgress);
    state.error = null;
    state.items = List.of();
    state.fluids = List.of();
    Level world = smeltery.getLevel();
    if (world == null) return;
    StructureData structure = smeltery.getStructure();
    state.structureValid = state.blockState.getValue(ControllerBlock.IN_STRUCTURE) && structure != null;
    BlockPos pos = smeltery.getBlockPos();

    // render erroring block, done whether in the structure or not
    BlockPos errorPos = smeltery.getErrorPos();
    if (errorPos != null && Minecraft.getInstance().player != null) {
      // either we must be holding the book, or the structure must be erroring and it be within 10 seconds of last update
      state.highlightError = smeltery.isHighlightError();
      if ((!state.structureValid && state.highlightError) || smeltery.showDebugBlockBorder(Minecraft.getInstance().player)) {
        // distance check, 512 is the squared length of the diagonal of a max size structure
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        int dx = playerPos.getX() - pos.getX();
        int dz = playerPos.getZ() - pos.getZ();
        if ((dx * dx + dz * dz) < 512) {
          state.error = errorPos.subtract(pos);
        }
      }
    }

    // if no structure, nothing else to do
    if (!state.structureValid) {
      return;
    }
    BlockPos minPos = structure.getMinInside();
    BlockPos maxPos = structure.getMaxInside();
    state.minPos = minPos;
    state.maxPos = maxPos;
    state.fluids = smeltery.getTank().getFluids().stream().map(FluidStack::copy).toList();
    state.capacity = smeltery.getTank().getCapacity();
    // use minPos for brightness
    state.fluidLight = LevelRenderer.getLightColor(world, minPos);
    state.facing = state.blockState.getValue(ControllerBlock.FACING);

    // items
    int max = Config.CLIENT.maxSmelteryItemQuads.get();
    if (max != 0) {
      int xd = 1 + maxPos.getX() - minPos.getX();
      int zd = 1 + maxPos.getZ() - minPos.getZ();
      int layer = xd * zd;
      MeltingModuleInventory inventory = smeltery.getMeltingInventory();
      List<StructureItem> items = new ArrayList<>();
      int quadsRendered = 0;
      for (int i = 0; i < inventory.getSlots(); i++) {
        ItemStack stack = inventory.getStackInSlot(i);
        if (!stack.isEmpty()) {
          // calculate position inside the smeltery from slot index
          int height = i / layer;
          int layerIndex = i % layer;
          int offsetX = layerIndex % xd;
          int offsetZ = layerIndex / xd;
          ItemStackRenderState item = new ItemStackRenderState();
          itemModelResolver.updateForTopItem(item, stack, TinkerItemDisplays.MELTER.getValue(), world, null, i);
          items.add(new StructureItem(item, offsetX, height, offsetZ, LevelRenderer.getLightColor(world, minPos.offset(offsetX, height, offsetZ))));
          // shortcut: quads are estimated per item, item render states do not expose their quads
          if (max != -1) {
            quadsRendered += QUADS_PER_ITEM;
            if (quadsRendered > max) {
              break;
            }
          }
        }
      }
      state.items = items;
    }
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    BlockPos error = state.error;
    if (error != null) {
      // color will be yellow if the structure is valid (expanding), red if invalid
      int color = ARGB.colorFromFloat(0.5f, 1f, state.structureValid ? 1f : 0f, 0f);
      collector.submitCustomGeometry(matrices, state.highlightError ? TinkerRenderTypes.ERROR_BLOCK : RenderTypes.lines(), (pose, buffer) -> {
        PoseStack stack = new PoseStack();
        stack.last().set(pose);
        ShapeRenderer.renderShape(stack, buffer, Shapes.block(), error.getX(), error.getY(), error.getZ(), color, 2f);
      });
    }
    if (!state.structureValid) {
      return;
    }

    // offset to make rendering min pos relative
    BlockPos pos = state.blockPos;
    BlockPos minPos = state.minPos;
    matrices.pushPose();
    matrices.translate(minPos.getX() - pos.getX(), minPos.getY() - pos.getY(), minPos.getZ() - pos.getZ());
    SmelteryTankRenderer.renderFluids(matrices, collector, state.fluids, state.capacity, minPos, state.maxPos, state.fluidLight);

    // render items
    Quaternionf itemRotation = Axis.YP.rotationDegrees(-90.0F * (float)state.facing.get2DDataValue());
    for (StructureItem item : state.items) {
      // offset to the slot position in the structure, scale, and rotate the item
      matrices.pushPose();
      matrices.translate(item.x + 0.5f, item.y + 0.5f, item.z + 0.5f);
      matrices.mulPose(itemRotation);
      matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
      item.item.submit(matrices, collector, item.light, OverlayTexture.NO_OVERLAY, 0);
      matrices.popPose();
    }
    matrices.popPose();
  }

  @Override
  public boolean shouldRenderOffScreen() {
    // structures are larger than the controller
    return true;
  }
}
