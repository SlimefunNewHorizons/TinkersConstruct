package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.item.ItemModelResolver;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.smeltery.block.entity.ProxyTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.ProxyItemTank;

import javax.annotation.Nullable;
import java.util.List;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Renderer for {@link ProxyTankBlockEntity}. Unlike {@link TankInventoryBlockEntityRenderer}, does not animate the fluid. */
public class ProxyTankBlockEntityRenderer implements BlockEntityRenderer<ProxyTankBlockEntity,TankRenderState> {
  private final ItemModelResolver itemModelResolver;
  public ProxyTankBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public TankRenderState createRenderState() {
    return new TankRenderState();
  }

  @Override
  public void extractRenderState(ProxyTankBlockEntity proxyTank, TankRenderState state, float partialTicks, Vec3 camera, @Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(proxyTank, state, partialTicks, camera, breakProgress);
    ProxyItemTank<?> itemTank = proxyTank.getItemTank();
    state.fluids = Config.CLIENT.tankFluidModel.get() ? List.of() : FluidCuboid.REGISTRY.get(state.blockState, List.of());
    state.fluid = itemTank.getFluidInTank(0).copy();
    state.capacity = itemTank.getTankCapacity(0);
    state.offset = 0;
    state.extractItems(itemModelResolver, itemTank::getStackInSlot, proxyTank.getLevel());
    state.facing = state.blockState.getValue(HORIZONTAL_FACING);
  }

  @Override
  public void submit(TankRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    state.submit(matrices, collector, true);
  }
}
