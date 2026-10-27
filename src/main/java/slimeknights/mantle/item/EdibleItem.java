package slimeknights.mantle.item;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;
import java.util.Objects;

public class EdibleItem extends Item {
  public EdibleItem(FoodProperties foodIn) {
    this(new Properties().food(foodIn));
  }

  public EdibleItem(Item.Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flagIn) {
    List<Component> tooltip = new ArrayList<>();
    try {
        TranslationHelper.addOptionalTooltip(stack, tooltip);
        // TODO: use ContainerFoodItem helper for more potion like effects?
        for (FoodProperties.PossibleEffect possibleEffect : Objects.requireNonNull(stack.getFoodProperties(null)).effects()) {
          MobEffectInstance effect = possibleEffect.effect();
          if (effect != null) {
            tooltip.add(Component.literal(I18n.get(effect.getDescriptionId()).trim()).withStyle(ChatFormatting.GRAY));
          }
        }
  
    } finally {
      tooltip.forEach(tooltipAdder);
    }
  }
}
