package slimeknights.tconstruct.smeltery.block.controller;

import slimeknights.mantle.block.IBlockEntityRemovalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;

import javax.annotation.Nullable;

public class MelterBlock extends TinyMultiblockControllerBlock implements IBlockEntityRemovalHandler {
  public MelterBlock(Properties props) {
    super(props);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return new MelterBlockEntity(pPos, pState);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> check) {
    return pLevel.isClientSide() ? null : BlockEntityHelper.castTicker(check, TinkerSmeltery.melter.get(), MelterBlockEntity.SERVER_TICKER);
  }

  @Override
  public void onBlockEntityRemoved(BlockState state, Level level, BlockPos pos, BlockState newState) {
    if (!newState.is(this)) {
      BlockEntityHelper.get(MelterBlockEntity.class, level, pos).ifPresent(melter -> dropInventory(level, pos, melter.getItemHandler()));
    }
  }


  /*
   * Display
   */

  @Deprecated
  @Override
  public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
    return 1.0F;
  }

  @Override
  protected boolean propagatesSkylightDown(BlockState state) {
    return true;
  }


  @Override
  public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
    if (state.getValue(ACTIVE)) {
      double x = pos.getX() + 0.5D;
      double y = (double) pos.getY() + (rand.nextFloat() * 6F) / 16F;
      double z = pos.getZ() + 0.5D;
      double frontOffset = 0.52D;
      double sideOffset = rand.nextDouble() * 0.6D - 0.3D;
      spawnFireParticles(world, state, x, y, z, frontOffset, sideOffset);
    }
  }
}
