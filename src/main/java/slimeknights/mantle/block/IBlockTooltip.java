package slimeknights.mantle.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.function.Consumer;

/**
 * Block adding tooltip lines to its block item. Blocks lost {@code appendHoverText} in 1.21.5, so Mantle forwards item tooltips to blocks implementing this.
 */
public interface IBlockTooltip {
  /** Adds tooltip lines for the block item stack */
  void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag);

  /** Event listener forwarding block item tooltips */
  static void onItemTooltip(ItemTooltipEvent event) {
    ItemStack stack = event.getItemStack();
    if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof IBlockTooltip block) {
      block.appendHoverText(stack, event.getContext(), stack.getOrDefault(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT), event.getToolTip()::add, event.getFlags());
    }
  }
}
