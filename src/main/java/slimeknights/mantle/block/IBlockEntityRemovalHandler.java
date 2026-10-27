package slimeknights.mantle.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block hook that runs when the block is replaced while its block entity still exists, called from
 * {@link slimeknights.mantle.block.entity.MantleBlockEntity#preRemoveSideEffects(BlockPos, BlockState)}.
 * Replaces {@code onRemove}, which 1.21.5 split into {@code BlockEntity#preRemoveSideEffects} and {@code affectNeighborsAfterRemoval}.
 * Implementing blocks handle their own drops, so the default container drops are skipped.
 */
public interface IBlockEntityRemovalHandler {
  /**
   * Called on the server when the block changes to a different block
   * @param state     Removed state
   * @param level     Level
   * @param pos       Block position
   * @param newState  State replacing the block
   */
  void onBlockEntityRemoved(BlockState state, Level level, BlockPos pos, BlockState newState);
}
