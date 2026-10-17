package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.melting.DamageableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeltingOutputTest extends BaseMcTest {
  private static MeltingRecipe recipe(List<FluidOutput> byproducts) {
    return new MeltingRecipe(TConstruct.getResource("test_outputs"), "", Ingredient.of(Items.IRON_INGOT),
      FluidOutput.fromFluid(Fluids.WATER, 90), 800, 1, byproducts, false);
  }

  private static SmelteryBlockEntity smeltery(int capacity) {
    SmelteryBlockEntity smeltery = new SmelteryBlockEntity(BlockPos.ZERO, TinkerSmeltery.smelteryController.get().defaultBlockState());
    smeltery.getTank().setCapacity(capacity);
    smeltery.getMeltingInventory().resize(1, stack -> {});
    smeltery.getMeltingInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    return smeltery;
  }

  private static FoundryBlockEntity foundry(int capacity) {
    FoundryBlockEntity foundry = new FoundryBlockEntity(BlockPos.ZERO, TinkerSmeltery.foundryController.get().defaultBlockState());
    foundry.getTank().setCapacity(capacity);
    foundry.getMeltingInventory().resize(1, stack -> {});
    foundry.getMeltingInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    return foundry;
  }

  @Test
  void insufficientCombinedSpaceLeavesAllOutputsAndInputUntouchedUntilRetry() {
    FoundryBlockEntity foundry = foundry(100);
    ByproductMeltingModuleInventory inventory = (ByproductMeltingModuleInventory)foundry.getMeltingInventory();
    IMeltingRecipe recipe = recipe(List.of(FluidOutput.fromFluid(Fluids.LAVA, 30)));

    assertThat(inventory.tryFillTank(0, recipe)).isFalse();
    assertThat(inventory.tryFillTank(0, recipe)).isFalse();
    assertThat(foundry.getTank().getContained()).isZero();
    assertThat(inventory.getStackInSlot(0).is(Items.IRON_INGOT)).isTrue();

    foundry.getTank().setCapacity(120);
    assertThat(inventory.tryFillTank(0, recipe)).isTrue();
    assertThat(foundry.getTank().getContained()).isEqualTo(120);
    assertThat(foundry.getTank().drain(new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE).getAmount()).isEqualTo(90);
    assertThat(foundry.getTank().drain(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE).getAmount()).isEqualTo(30);
  }

  @Test
  void allByproductsCountAgainstTheSameCapacityIncludingRepeatedFluids() {
    FoundryBlockEntity foundry = foundry(140);
    ByproductMeltingModuleInventory inventory = (ByproductMeltingModuleInventory)foundry.getMeltingInventory();
    IMeltingRecipe recipe = recipe(List.of(FluidOutput.fromFluid(Fluids.WATER, 30), FluidOutput.fromFluid(Fluids.LAVA, 30)));
    foundry.getTank().fill(new FluidStack(Fluids.LAVA, 10), FluidAction.EXECUTE);

    assertThat(inventory.tryFillTank(0, recipe)).isFalse();
    assertThat(foundry.getTank().getContained()).isEqualTo(10);
    foundry.getTank().setCapacity(160);
    assertThat(inventory.tryFillTank(0, recipe)).isTrue();
    assertThat(foundry.getTank().getContained()).isEqualTo(160);
  }

  @Test
  void smelteryOnlyRequiresSpaceForTheMainOutput() {
    SmelteryBlockEntity smeltery = smeltery(89);
    MeltingModuleInventory inventory = smeltery.getMeltingInventory();
    IMeltingRecipe recipe = recipe(List.of(FluidOutput.fromFluid(Fluids.LAVA, 30)));

    assertThat(inventory.tryFillTank(0, recipe)).isFalse();
    assertThat(smeltery.getTank().getContained()).isZero();
    smeltery.getTank().setCapacity(90);
    assertThat(inventory.tryFillTank(0, recipe)).isTrue();
    assertThat(smeltery.getTank().getContained()).isEqualTo(90);
    assertThat(smeltery.getTank().drain(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE).isEmpty()).isTrue();
  }

  @Test
  void damageScaledByproductsDetermineRequiredSpace() {
    FoundryBlockEntity foundry = foundry(60);
    ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
    tool.setDamageValue(tool.getMaxDamage() / 2);
    foundry.getMeltingInventory().setStackInSlot(0, tool);
    DamageableMeltingRecipe recipe = new DamageableMeltingRecipe(TConstruct.getResource("test_damaged"), "",
      Ingredient.of(Items.IRON_PICKAXE), FluidOutput.fromFluid(Fluids.WATER, 90), 800, 1,
      List.of(FluidOutput.fromFluid(Fluids.LAVA, 200)), 10, List.of(25));
    ByproductMeltingModuleInventory inventory = (ByproductMeltingModuleInventory)foundry.getMeltingInventory();

    assertThat(inventory.tryFillTank(0, recipe)).isFalse();
    assertThat(foundry.getTank().getContained()).isZero();
    foundry.getTank().setCapacity(140);
    assertThat(inventory.tryFillTank(0, recipe)).isTrue();
    assertThat(foundry.getTank().getContained()).isEqualTo(140);
  }

  @Test
  void melterInventoryProducesOnlyTheMainOutput() {
    SmelteryBlockEntity parent = smeltery(90);
    MeltingModuleInventory inventory = new MeltingModuleInventory(parent, parent.getTank(), Config.COMMON.melterOreRate, 1);
    assertThat(inventory.tryFillTank(0, recipe(List.of(FluidOutput.fromFluid(Fluids.LAVA, 30))))).isTrue();
    assertThat(parent.getTank().getContained()).isEqualTo(90);
  }

  @Test
  void unresolvedMainOutputCannotConsumeInputOrProduceByproducts() {
    FoundryBlockEntity parent = foundry(1000);
    MeltingRecipe recipe = new MeltingRecipe(TConstruct.getResource("test_empty_output"), "", Ingredient.of(Items.IRON_INGOT),
      FluidOutput.EMPTY, 800, 1, List.of(FluidOutput.fromFluid(Fluids.LAVA, 30)), false);
    ByproductMeltingModuleInventory inventory = (ByproductMeltingModuleInventory)parent.getMeltingInventory();
    assertThat(inventory.tryFillTank(0, recipe)).isFalse();
    assertThat(parent.getTank().getContained()).isZero();
    MeltingModuleInventory melter = new MeltingModuleInventory(parent, parent.getTank(), Config.COMMON.melterOreRate, 1);
    assertThat(melter.tryFillTank(0, recipe)).isFalse();
  }
}
