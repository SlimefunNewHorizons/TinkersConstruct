package slimeknights.tconstruct.smeltery.block.entity.module;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

import java.util.List;

public class ByproductMeltingModuleInventory extends MeltingModuleInventory {
  private final SmelteryTank<?> tank;

  public ByproductMeltingModuleInventory(MantleBlockEntity parent, SmelteryTank<?> tank, IOreRate oreRate, int size) {
    super(parent, tank, oreRate, size);
    this.tank = tank;
  }

  public ByproductMeltingModuleInventory(MantleBlockEntity parent, SmelteryTank<?> tank, IOreRate oreRate) {
    this(parent, tank, oreRate, 0);
  }

  @Override
  protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
    MeltingModule module = getModule(index);
    FluidStack output = recipe.getOutput(module);
    if (output.isEmpty()) {
      return false;
    }
    List<FluidStack> byproducts = recipe.getByproducts(module);
    long required = output.getAmount();
    for (FluidStack byproduct : byproducts) {
      required += byproduct.getAmount();
    }
    // All fluids share one capacity. Simulating each fill separately would reuse the same free space.
    if (required > tank.getRemainingSpace()) {
      return false;
    }
    tank.fill(output, FluidAction.EXECUTE);
    for (FluidStack byproduct : byproducts) {
      tank.fill(byproduct, FluidAction.EXECUTE);
    }
    return true;
  }
}
