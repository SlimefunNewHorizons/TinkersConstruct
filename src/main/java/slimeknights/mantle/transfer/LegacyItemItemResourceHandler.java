package slimeknights.mantle.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Function;

/**
 * Exposes a legacy {@link IItemHandler} stored inside an item as an item {@link ResourceHandler}.
 * Each change runs the legacy handler on a copy of one item, then swaps the copy into the {@link ItemAccess}, which handles the transaction.
 */
@SuppressWarnings("removal")
public class LegacyItemItemResourceHandler implements ResourceHandler<ItemResource> {
  private final ItemAccess access;
  /** Creates a handler editing the given stack in place */
  private final Function<ItemStack, ? extends IItemHandler> factory;

  public LegacyItemItemResourceHandler(ItemAccess access, Function<ItemStack, ? extends IItemHandler> factory) {
    this.access = access;
    this.factory = factory;
  }

  /** Creates a legacy handler for a single copy of the current item */
  private IItemHandler view() {
    return factory.apply(access.getResource().toStack(1));
  }

  @Override
  public int size() {
    return view().getSlots();
  }

  @Override
  public ItemResource getResource(int index) {
    return ItemResource.of(view().getStackInSlot(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return view().getStackInSlot(index).getCount();
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    int limit = view().getSlotLimit(index);
    return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return resource.isEmpty() || view().isItemValid(index, resource.toStack());
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || access.getAmount() == 0) {
      return 0;
    }
    ItemStack copy = access.getResource().toStack(1);
    ItemStack remainder = factory.apply(copy).insertItem(index, resource.toStack(amount), false);
    int inserted = amount - remainder.getCount();
    return inserted > 0 && access.exchange(ItemResource.of(copy), 1, transaction) == 1 ? inserted : 0;
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || access.getAmount() == 0) {
      return 0;
    }
    ItemStack copy = access.getResource().toStack(1);
    IItemHandler handler = factory.apply(copy);
    if (!resource.matches(handler.getStackInSlot(index))) {
      return 0;
    }
    ItemStack extracted = handler.extractItem(index, amount, false);
    return !extracted.isEmpty() && access.exchange(ItemResource.of(copy), 1, transaction) == 1 ? extracted.getCount() : 0;
  }
}
