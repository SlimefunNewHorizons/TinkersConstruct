package slimeknights.mantle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import slimeknights.mantle.util.TranslationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Armor item with automatic tooltip support. Armor is a plain item with components since 1.21.5 */
public class ArmorTooltipItem extends Item {

  public ArmorTooltipItem(ArmorMaterial armorMaterial, ArmorType type, Properties builder) {
    super(builder.humanoidArmor(armorMaterial, type));
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flagIn) {
    List<Component> tooltip = new ArrayList<>();
    TranslationHelper.addOptionalTooltip(stack, tooltip);
    tooltip.forEach(tooltipAdder);
    super.appendHoverText(stack, context, display, tooltipAdder, flagIn);
  }
}
