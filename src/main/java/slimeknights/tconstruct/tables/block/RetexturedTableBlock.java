package slimeknights.tconstruct.tables.block;

import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import slimeknights.mantle.block.IBlockTooltip;
import slimeknights.mantle.block.IBlockEntityRemovalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.InventoryBlock;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public abstract class RetexturedTableBlock extends TabbedTableBlock implements IBlockEntityRemovalHandler, IBlockTooltip {
  public RetexturedTableBlock(Properties builder) {
    super(builder);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
    List<Component> tooltip = new ArrayList<>();
    try {
        RetexturedHelper.addTooltip(stack, tooltip, flag);
  
    } finally {
      tooltip.forEach(tooltipAdder);
    }
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(world, pos, state, placer, stack);
    RetexturedBlock.updateTextureBlock(world, pos, stack);
  }

  @Override
  protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return RetexturedBlock.getPickBlock(world, pos, state);
  }

  /**
   * Drops table inputs directly from the block entity. Table inventories intentionally do not expose
   * automation capabilities (see {@code TinkerTables#registerCapabilities}), so {@link InventoryBlock}'s
   * capability lookup in {@code onRemove} finds no handler and loses their contents when the block is broken.
   */
  @Override
  public void onBlockEntityRemoved(BlockState state, Level level, BlockPos pos, BlockState newState) {
    if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof TableBlockEntity table) {
      InventoryBlock.dropInventoryItems(level, pos, table.getItemHandler());
    }
  }
}
