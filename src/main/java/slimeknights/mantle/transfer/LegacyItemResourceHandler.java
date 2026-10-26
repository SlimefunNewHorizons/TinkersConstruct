package slimeknights.mantle.transfer;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a legacy {@link IItemHandler} as a transactional {@link ResourceHandler}.
 * Changes apply to the handler immediately and are undone with the inverse operation if the transaction is aborted.
 */
@SuppressWarnings("removal")
public class LegacyItemResourceHandler extends SnapshotJournal<Integer> implements ResourceHandler<ItemResource> {
  /** Wrapped handler */
  @Getter
  private final IItemHandler handler;
  /** Changes made in the open transactions, in order */
  private final List<Change> changes = new ArrayList<>();

  public LegacyItemResourceHandler(IItemHandler handler) {
    this.handler = handler;
  }

  @Override
  public int size() {
    return handler.getSlots();
  }

  @Override
  public ItemResource getResource(int index) {
    return ItemResource.of(handler.getStackInSlot(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return handler.getStackInSlot(index).getCount();
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    int limit = handler.getSlotLimit(index);
    return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return resource.isEmpty() || handler.isItemValid(index, resource.toStack());
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0) {
      return 0;
    }
    updateSnapshots(transaction);
    ItemStack remainder = handler.insertItem(index, resource.toStack(amount), false);
    int inserted = amount - remainder.getCount();
    if (inserted > 0) {
      changes.add(new Change(index, resource.toStack(inserted), true));
    }
    return inserted;
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || !resource.matches(handler.getStackInSlot(index))) {
      return 0;
    }
    updateSnapshots(transaction);
    ItemStack extracted = handler.extractItem(index, amount, false);
    if (!extracted.isEmpty()) {
      changes.add(new Change(index, extracted.copy(), false));
    }
    return extracted.getCount();
  }

  @Override
  protected Integer createSnapshot() {
    return changes.size();
  }

  @Override
  protected void revertToSnapshot(Integer snapshot) {
    for (int i = changes.size() - 1; i >= snapshot; i--) {
      Change change = changes.remove(i);
      if (change.inserted) {
        handler.extractItem(change.index, change.stack.getCount(), false);
      } else {
        handler.insertItem(change.index, change.stack.copy(), false);
      }
    }
  }

  @Override
  protected void onRootCommit(Integer originalState) {
    changes.clear();
  }

  /** Change to undo on abort */
  private record Change(int index, ItemStack stack, boolean inserted) {}
}
