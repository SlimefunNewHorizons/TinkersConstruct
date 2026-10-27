package slimeknights.mantle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.mantle.block.IBlockEntityRemovalHandler;
import slimeknights.mantle.util.ValueIOHelper;

import javax.annotation.Nullable;

public class MantleBlockEntity extends BlockEntity {

  public MantleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public boolean isClient() {
    return this.getLevel() != null && this.getLevel().isClientSide();
  }

  /**
   * Marks the chunk dirty without performing comparator updates (twice!!) or block state checks
   * Used since most of our markDirty calls only adjust TE data
   */
  @SuppressWarnings("deprecation")
  public void setChangedFast() {
    if (level != null) {
      if (level.hasChunkAt(worldPosition)) {
        level.getChunkAt(worldPosition).markUnsaved();
      }
    }
  }

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (level != null && state.getBlock() instanceof IBlockEntityRemovalHandler handler) {
      handler.onBlockEntityRemoved(state, level, pos, level.getBlockState(pos));
    } else {
      super.preRemoveSideEffects(pos, state);
    }
  }

  /** Gets registries for NBT serialization */
  protected HolderLookup.Provider registries() {
    return ValueIOHelper.registries(level);
  }


  /* Syncing */

  /**
   * If true, this TE syncs when {@link net.minecraft.world.level.Level#blockUpdated(BlockPos, Block) is called
   * Syncs data from {@link #saveSynced(CompoundTag)}
   */
  protected boolean shouldSyncOnUpdate() {
    return false;
  }

  @Override
  @Nullable
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    // number is just used for vanilla, -1 ensures it skips all instanceof checks as its not a vanilla TE
    return shouldSyncOnUpdate() ? ClientboundBlockEntityDataPacket.create(this) : null;
  }

  /**
   * Write to NBT that is synced to the client and persisted by {@link #saveAdditional(CompoundTag, HolderLookup.Provider)}.
   * @param nbt  NBT
   */
  protected void saveSynced(CompoundTag nbt) {}

  /** Registry-aware synchronized data hook used by 1.21 component codecs. */
  protected void saveSynced(CompoundTag nbt, HolderLookup.Provider registries) {
    saveSynced(nbt);
  }

  /** Transitional hook for block entities still using the pre-1.21 load signature. */
  @Deprecated
  public void load(CompoundTag nbt) {}

  /** Registry-aware transitional load hook. */
  public void load(CompoundTag nbt, HolderLookup.Provider registries) {
    load(nbt);
  }

  /** NBT load hook, called from {@link #loadAdditional(ValueInput)} with all saved values */
  protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
    load(nbt, registries);
  }

  /** NBT save hook, all values written are merged into {@link #saveAdditional(ValueOutput)} */
  protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
    saveSynced(nbt, registries);
  }

  @Override
  protected final void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    loadAdditional(ValueIOHelper.read(input), input.lookup());
  }

  @Override
  protected final void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    CompoundTag nbt = new CompoundTag();
    saveAdditional(nbt, registries());
    ValueIOHelper.write(output, nbt);
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag nbt = new CompoundTag();
    saveSynced(nbt, registries);
    return nbt;
  }
}
