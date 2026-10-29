package slimeknights.tconstruct.tables.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import slimeknights.tconstruct.shared.block.TableBlock;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.chest.AbstractChestBlockEntity;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression test for table and chest inventories vanishing when the block is broken. Mantle's
 * {@code InventoryBlock#onRemove} looks the item capability up by the block state at the position, which is already the
 * new block by then, so it never finds a handler; tables additionally do not register the capability at all.
 */
@SuppressWarnings("deprecation")
class TableBlockInventoryDropTest extends BaseMcTest {
  private static final BlockPos POS = new BlockPos(0, 0, 0);

  @Test
  void tableBlocksDropInventoryWhenBroken() {
    for (TableBlock block : new TableBlock[] {
      TinkerTables.tinkerStation.get(), TinkerTables.craftingStation.get(), TinkerTables.partBuilder.get(),
      TinkerTables.modifierWorktable.get(), TinkerTables.tinkersAnvil.get(), TinkerTables.scorchedAnvil.get() }) {
      assertDropsInventory(block);
    }
  }

  @Test
  void chestBlocksDropInventoryWhenBroken() {
    for (TableBlock block : new TableBlock[] { TinkerTables.castChest.get(), TinkerTables.partChest.get(), TinkerTables.tinkersChest.get() }) {
      IChestItemHandler handler = mock(IChestItemHandler.class);
      when(handler.getSlots()).thenReturn(2);
      when(handler.getStackInSlot(0)).thenReturn(new ItemStack(Items.GOLD_INGOT));
      when(handler.getStackInSlot(1)).thenReturn(ItemStack.EMPTY);
      AbstractChestBlockEntity chest = mock(AbstractChestBlockEntity.class);
      when(chest.getItemHandler()).thenReturn(handler);
      Level level = mock(Level.class);
      when(level.getBlockEntity(POS)).thenReturn(chest);
      ArgumentCaptor<ItemStack> dropped = ArgumentCaptor.forClass(ItemStack.class);

      try (MockedStatic<Containers> containers = Mockito.mockStatic(Containers.class)) {
        block.onBlockEntityRemoved(block.defaultBlockState(), level, POS, Blocks.AIR.defaultBlockState());

        containers.verify(() -> Containers.dropItemStack(eq(level), anyDouble(), anyDouble(), anyDouble(), dropped.capture()), Mockito.atLeastOnce());
      }

      List<Item> items = dropped.getAllValues().stream().map(ItemStack::getItem).toList();
      assertThat(items).as("%s drops its inventory", block).contains(Items.GOLD_INGOT);
    }
  }

  @Test
  void replacingWithAnotherStateOfSameBlockDoesNotDropInventory() {
    TableBlock block = TinkerTables.tinkerStation.get();
    Level level = levelWithTable(new ItemStackHandler(4));

    try (MockedStatic<Containers> containers = Mockito.mockStatic(Containers.class)) {
      block.onBlockEntityRemoved(block.defaultBlockState(), level, POS, block.defaultBlockState());
      containers.verifyNoInteractions();
    }
  }

  /** Breaks the given table and asserts the block entity inventory was dropped into the level */
  private static void assertDropsInventory(TableBlock block) {
    ItemStackHandler handler = new ItemStackHandler(4);
    handler.setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
    handler.setStackInSlot(3, new ItemStack(Items.DIRT));
    Level level = levelWithTable(handler);
    ArgumentCaptor<ItemStack> dropped = ArgumentCaptor.forClass(ItemStack.class);

    try (MockedStatic<Containers> containers = Mockito.mockStatic(Containers.class)) {
      block.onBlockEntityRemoved(block.defaultBlockState(), level, POS, Blocks.AIR.defaultBlockState());

      // dropped once per slot, including empty slots
      containers.verify(() -> Containers.dropItemStack(eq(level), anyDouble(), anyDouble(), anyDouble(), dropped.capture()), Mockito.atLeastOnce());
    }

    List<Item> items = dropped.getAllValues().stream().map(ItemStack::getItem).toList();
    assertThat(items).as("%s drops its inventory", block).contains(Items.DIAMOND, Items.DIRT);
  }

  /** Creates a level whose block entity at {@link #POS} owns the given inventory */
  private static Level levelWithTable(IItemHandlerModifiable handler) {
    TableBlockEntity table = mock(TableBlockEntity.class);
    when(table.getItemHandler()).thenReturn(handler);
    Level level = mock(Level.class);
    when(level.getBlockEntity(POS)).thenReturn(table);
    return level;
  }
}
