package slimeknights.tconstruct.smeltery.item;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.tconstruct.TConstruct;

import java.util.List;

/** Item for creating casts that looks like a tool part */
public class DummyMaterialItem extends Item {
  private static final Component DUMMY_TOOL_PART = TConstruct.makeTranslation("item", "dummy_tool_part.tooltip").withStyle(ChatFormatting.GRAY);
  public DummyMaterialItem(Properties pProperties) {
    super(pProperties);
  }

  @Override
  public void appendHoverText(ItemStack pStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag pIsAdvanced) {
    List<Component> tooltip = new ArrayList<>();
    try {
        tooltip.add(DUMMY_TOOL_PART);
  
    } finally {
      tooltip.forEach(tooltipAdder);
    }
  }
}
