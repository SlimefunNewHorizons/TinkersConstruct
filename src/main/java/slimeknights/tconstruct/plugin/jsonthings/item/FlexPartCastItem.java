package slimeknights.tconstruct.plugin.jsonthings.item;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.items.FlexItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.tools.part.PartCastItem;

import java.util.List;
import java.util.function.Supplier;

/** Tool part cast item, which adds the part cost to the tooltip */
public class FlexPartCastItem extends FlexItem {
  private final Supplier<Item> part;
  public FlexPartCastItem(Properties properties, ItemBuilder builder, Supplier<Item> part) {
    super(properties, builder);
    this.part = part;
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
    List<Component> tooltip = new ArrayList<>();
    try {
        int cost = MaterialCastingLookup.getItemCost(part.get());
        if (cost > 0) {
          tooltip.add(Component.translatable(PartCastItem.COST_KEY, cost).withStyle(ChatFormatting.GRAY));
        }
  
    } finally {
      tooltip.forEach(tooltipAdder);
    }
  }
}
