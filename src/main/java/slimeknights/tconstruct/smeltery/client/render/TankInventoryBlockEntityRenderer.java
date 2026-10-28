package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity.ITankInventoryBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class TankInventoryBlockEntityRenderer<T extends BlockEntity & ITankInventoryBlockEntity> implements BlockEntityRenderer<T,TankRenderState> {
  private final ItemModelResolver itemModelResolver;
  private final EnumProperty<Direction> directionProperty;
  public TankInventoryBlockEntityRenderer(Context context, EnumProperty<Direction> directionProperty) {
    this.itemModelResolver = context.itemModelResolver();
    this.directionProperty = directionProperty;
  }

  @Override
  public TankRenderState createRenderState() {
    return new TankRenderState();
  }

  @Override
  public void extractRenderState(T melter, TankRenderState state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(melter, state, partialTicks, camera, breakProgress);
    state.extractTank(melter.getTank(), Config.CLIENT.tankFluidModel.get() ? List.of() : FluidCuboid.REGISTRY.get(state.blockState, List.of()), partialTicks);
    // TODO: can we show count somehow?
    state.extractItems(itemModelResolver, i -> melter.getItemHandler().getStackInSlot(i), melter.getLevel());
    state.facing = state.blockState.getValue(directionProperty);
  }

  @Override
  public void submit(TankRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    state.submit(matrices, collector, true);
  }
}
