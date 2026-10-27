package slimeknights.tconstruct.smeltery.block.component;

import slimeknights.mantle.block.IBlockEntityRemovalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.StateArgumentPredicate;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathType;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import javax.annotation.Nullable;

public class SearedBlock extends Block implements EntityBlock, IBlockEntityRemovalHandler {
  public static final BooleanProperty IN_STRUCTURE = BooleanProperty.create("in_structure");
  public static final StateArgumentPredicate<EntityType<?>> VALID_SPAWN = (s, r, p, e) -> !s.hasProperty(SearedBlock.IN_STRUCTURE) || !s.getValue(SearedBlock.IN_STRUCTURE);

  protected final boolean requiredBlockEntity;
  public SearedBlock(Properties properties, boolean requiredBlockEntity) {
    super(properties);
    this.requiredBlockEntity = requiredBlockEntity;
    this.registerDefaultState(this.defaultBlockState().setValue(IN_STRUCTURE, false));
  }

  @Override
  protected void createBlockStateDefinition(Builder<Block,BlockState> builder) {
    builder.add(IN_STRUCTURE);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    if (requiredBlockEntity || state.getValue(IN_STRUCTURE)) {
      return new SmelteryComponentBlockEntity(pos, state);
    }
    return null;
  }

  @Override
  public void onBlockEntityRemoved(BlockState oldState, Level world, BlockPos pos, BlockState newState) {
    // block changed, tell the master; the chunk removes the block entity afterwards
    if (requiredBlockEntity || oldState.getValue(IN_STRUCTURE)) {
      BlockEntityHelper.get(SmelteryComponentBlockEntity.class, world, pos).ifPresent(te -> te.notifyMasterOfChange(pos, newState));
    }
  }

  @Override
  protected void onPlace(BlockState newState, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
    super.onPlace(newState, world, pos, oldState, movedByPiston);
    // if the block is unchanged but left the structure, remove the block entity as we no longer need one
    if (oldState.is(this) && !requiredBlockEntity && oldState.getValue(IN_STRUCTURE) && !newState.getValue(IN_STRUCTURE)) {
      world.removeBlockEntity(pos);
    }
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    SmelteryComponentBlockEntity.updateNeighbors(world, pos, state);
  }

  @Override
  @Deprecated
  public boolean triggerEvent(BlockState state, Level worldIn, BlockPos pos, int id, int param) {
    super.triggerEvent(state, worldIn, pos, id, param);
    BlockEntity be = worldIn.getBlockEntity(pos);
    return be != null && be.triggerEvent(id, param);
  }

  @Nullable
  @Override
  public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
    return state.getValue(IN_STRUCTURE) ? PathType.DAMAGE_FIRE : PathType.OPEN;
  }
}
