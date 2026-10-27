package slimeknights.mantle.item;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Food item with a container that is returned when the item is consumed. Supports eating stackable items with containers.
 * Technically also works for items with no container.
 */
@SuppressWarnings("unused") // API
public class ContainerFoodItem extends Item {
  private final ItemUseAnimation useAnim;
  public ContainerFoodItem(Properties props, ItemUseAnimation useAnim) {
    super(props);
    this.useAnim = useAnim;
  }

  public ContainerFoodItem(Properties props) {
    this(props, ItemUseAnimation.DRINK);
  }

  @Override
  public ItemUseAnimation getUseAnimation(ItemStack pStack) {
    return useAnim;
  }

  /** Gets the status effects applied when consuming the stack, they live in the consumable component since 1.21.2 */
  public static List<MobEffectInstance> getConsumeEffects(ItemStack stack) {
    Consumable consumable = stack.get(DataComponents.CONSUMABLE);
    if (consumable == null) {
      return List.of();
    }
    List<MobEffectInstance> effects = new ArrayList<>();
    for (ConsumeEffect effect : consumable.onConsumeEffects()) {
      if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
        effects.addAll(apply.effects());
      }
    }
    return effects;
  }

  /** Adds effects to the tooltip */
  public static void addEffectTooltip(ItemStack stack, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (MobEffectInstance effect : getConsumeEffects(stack)) {
      {
        MutableComponent mutable = Component.translatable(effect.getDescriptionId());
        if (effect.getAmplifier() > 0) {
          mutable = Component.translatable("potion.withAmplifier", mutable, Component.translatable("potion.potency." + effect.getAmplifier()));
        }
        if (effect.getDuration() > 20) {
          mutable = Component.translatable("potion.withDuration", mutable, MobEffectUtil.formatDuration(effect, 1.0f, 20.0f));
        }
        tooltip.add(mutable.withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
      }
    }
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flagIn) {
    List<Component> tooltip = new ArrayList<>();
    try {
        addEffectTooltip(stack, tooltip);
  
    } finally {
      tooltip.forEach(tooltipAdder);
    }
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    ItemStack container = stack.getCraftingRemainder();
    ItemStack result = super.finishUsingItem(stack, level, living);
    Player player = living instanceof Player p ? p : null;
    if (!container.isEmpty() && (player == null || !player.getAbilities().instabuild)) {
      container = container.copy();
      if (result.isEmpty()) {
        return container;
      }
      if (player != null) {
        if (!player.getInventory().add(container)) {
          player.drop(container, false);
        }
      }
    }
    return result;
  }

  /** Fluid containing variant of {@link ContainerFoodItem} */
  public static class FluidContainerFoodItem extends ContainerFoodItem {
    private final Supplier<FluidStack> fluid;
    public FluidContainerFoodItem(Properties props, Supplier<FluidStack> fluid) {
      super(props);
      this.fluid = fluid;
    }

    /** Creates the per-stack fluid capability registered by Mantle. */
    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
      return new ConstantFluidContainerWrapper(fluid.get(), stack);
    }
  }
}
