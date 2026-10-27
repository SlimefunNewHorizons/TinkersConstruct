package slimeknights.tconstruct.tables.block.entity.inventory;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/** Interface for tinker chest TEs */
public interface IChestItemHandler extends IItemHandlerModifiable, IScalingContainer {
  /** Sets the parent of this block */
  void setParent(MantleBlockEntity parent);
}
