package slimeknights.mantle.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Function;

/**
 * Exposes a legacy {@link IFluidHandlerItem} as an item fluid {@link ResourceHandler}.
 * Each change runs the legacy handler on a copy of one item, then swaps the result into the {@link ItemAccess}, which handles the transaction.
 */
@SuppressWarnings("removal")
public class LegacyFluidItemResourceHandler implements ResourceHandler<FluidResource> {
  private final ItemAccess access;
  private final Function<ItemStack, ? extends IFluidHandlerItem> factory;

  public LegacyFluidItemResourceHandler(ItemAccess access, Function<ItemStack, ? extends IFluidHandlerItem> factory) {
    this.access = access;
    this.factory = factory;
  }

  /** Creates a legacy handler for a single copy of the current item */
  private IFluidHandlerItem view() {
    return factory.apply(access.getResource().toStack(1));
  }

  @Override
  public int size() {
    return view().getTanks();
  }

  @Override
  public FluidResource getResource(int index) {
    return FluidResource.of(view().getFluidInTank(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return view().getFluidInTank(index).getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    return view().getTankCapacity(index);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return resource.isEmpty() || view().isFluidValid(index, resource.toStack(1));
  }

  /** Swaps the current item for the handler's container, returning false if the access rejected it */
  private boolean exchange(IFluidHandlerItem handler, TransactionContext transaction) {
    ItemStack container = handler.getContainer();
    if (container.isEmpty()) {
      return access.extract(access.getResource(), 1, transaction) == 1;
    }
    return access.exchange(ItemResource.of(container), 1, transaction) == 1;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return index == 0 ? insert(resource, amount, transaction) : 0;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || access.getAmount() == 0) {
      return 0;
    }
    IFluidHandlerItem handler = view();
    int filled = handler.fill(resource.toStack(amount), FluidAction.EXECUTE);
    return filled > 0 && exchange(handler, transaction) ? filled : 0;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return index == 0 ? extract(resource, amount, transaction) : 0;
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || access.getAmount() == 0) {
      return 0;
    }
    IFluidHandlerItem handler = view();
    FluidStack drained = handler.drain(resource.toStack(amount), FluidAction.EXECUTE);
    return !drained.isEmpty() && exchange(handler, transaction) ? drained.getAmount() : 0;
  }
}
