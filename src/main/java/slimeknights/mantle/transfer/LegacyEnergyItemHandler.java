package slimeknights.mantle.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Function;

/**
 * Exposes a legacy {@link IEnergyStorage} stored inside an item as an {@link EnergyHandler}.
 * Each change runs the legacy storage on a copy of one item, then swaps the copy into the {@link ItemAccess}, which handles the transaction.
 */
@SuppressWarnings("removal")
public class LegacyEnergyItemHandler implements EnergyHandler {
  private final ItemAccess access;
  /** Creates a storage editing the given stack in place */
  private final Function<ItemStack, ? extends IEnergyStorage> factory;

  public LegacyEnergyItemHandler(ItemAccess access, Function<ItemStack, ? extends IEnergyStorage> factory) {
    this.access = access;
    this.factory = factory;
  }

  /** Creates a legacy storage for a single copy of the current item */
  private IEnergyStorage view() {
    return factory.apply(access.getResource().toStack(1));
  }

  @Override
  public long getAmountAsLong() {
    return view().getEnergyStored();
  }

  @Override
  public long getCapacityAsLong() {
    return view().getMaxEnergyStored();
  }

  @Override
  public int insert(int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonNegative(amount);
    if (amount == 0 || access.getAmount() == 0) {
      return 0;
    }
    ItemStack copy = access.getResource().toStack(1);
    int inserted = factory.apply(copy).receiveEnergy(amount, false);
    return inserted > 0 && access.exchange(ItemResource.of(copy), 1, transaction) == 1 ? inserted : 0;
  }

  @Override
  public int extract(int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonNegative(amount);
    if (amount == 0 || access.getAmount() == 0) {
      return 0;
    }
    ItemStack copy = access.getResource().toStack(1);
    int extracted = factory.apply(copy).extractEnergy(amount, false);
    return extracted > 0 && access.exchange(ItemResource.of(copy), 1, transaction) == 1 ? extracted : 0;
  }
}
