package slimeknights.mantle.transfer;

import lombok.Getter;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a legacy {@link IFluidHandler} as a transactional {@link ResourceHandler}.
 * Changes apply to the handler immediately and are undone with the inverse operation if the transaction is aborted.
 */
@SuppressWarnings("removal")
public class LegacyFluidResourceHandler extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
  /** Wrapped handler */
  @Getter
  private final IFluidHandler handler;
  /** Changes made in the open transactions, in order */
  private final List<Change> changes = new ArrayList<>();

  public LegacyFluidResourceHandler(IFluidHandler handler) {
    this.handler = handler;
  }

  @Override
  public int size() {
    return handler.getTanks();
  }

  @Override
  public FluidResource getResource(int index) {
    return FluidResource.of(handler.getFluidInTank(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return handler.getFluidInTank(index).getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    return handler.getTankCapacity(index);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return resource.isEmpty() || handler.isFluidValid(index, resource.toStack(1));
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    // legacy handlers cannot target a tank, so the whole handler is filled through the first index
    return index == 0 ? insert(resource, amount, transaction) : 0;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0) {
      return 0;
    }
    updateSnapshots(transaction);
    int filled = handler.fill(resource.toStack(amount), FluidAction.EXECUTE);
    if (filled > 0) {
      changes.add(new Change(resource.toStack(filled), true));
    }
    return filled;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    return resource.matches(handler.getFluidInTank(index)) ? extract(resource, amount, transaction) : 0;
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0) {
      return 0;
    }
    updateSnapshots(transaction);
    FluidStack drained = handler.drain(resource.toStack(amount), FluidAction.EXECUTE);
    if (!drained.isEmpty()) {
      changes.add(new Change(drained.copy(), false));
    }
    return drained.getAmount();
  }

  @Override
  protected Integer createSnapshot() {
    return changes.size();
  }

  @Override
  protected void revertToSnapshot(Integer snapshot) {
    for (int i = changes.size() - 1; i >= snapshot; i--) {
      Change change = changes.remove(i);
      if (change.filled) {
        handler.drain(change.fluid.copy(), FluidAction.EXECUTE);
      } else {
        handler.fill(change.fluid.copy(), FluidAction.EXECUTE);
      }
    }
  }

  @Override
  protected void onRootCommit(Integer originalState) {
    changes.clear();
  }

  /** Change to undo on abort */
  private record Change(FluidStack fluid, boolean filled) {}
}
