package slimeknights.tconstruct.gadgets.block;

import slimeknights.mantle.block.IBlockTooltip;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.tconstruct.fluids.item.ContainerFoodItem;

import java.util.List;

/**
 * Extension of cake that utilizes a food instance for properties
 */
public class FoodCakeBlock extends CakeBlock implements IBlockTooltip {
  private final FoodProperties food;
  /** Effects from the consumable, food effects moved to consumables in 1.21.2 */
  private final List<ApplyStatusEffectsConsumeEffect> effects;
  private final EffectCombination combination;

  public FoodCakeBlock(Properties properties, FoodProperties food, Consumable consumable, EffectCombination combination) {
    super(properties);
    this.food = food;
    this.effects = consumable.onConsumeEffects().stream().filter(effect -> effect instanceof ApplyStatusEffectsConsumeEffect).map(effect -> (ApplyStatusEffectsConsumeEffect) effect).toList();
    this.combination = combination;
  }

  /** Gets all effects the cake may apply */
  private List<MobEffectInstance> allEffects() {
    return effects.stream().flatMap(effect -> effect.effects().stream()).toList();
  }

  @Override
  public void appendHoverText(ItemStack pStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag pFlag) {
    List<Component> tooltip = new ArrayList<>();
    try {
        ContainerFoodItem.addEffectTooltip(allEffects(), tooltip);
  
    } finally {
      tooltip.forEach(tooltipAdder);
    }
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    InteractionResult result = this.eatSlice(world, pos, state, player);
    if (result.consumesAction()) {
      return result;
    }
    if (world.isClientSide() && player.getMainHandItem().isEmpty()) {
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  /** Checks if the given player has all potion effects from the food */
  private boolean hasAllEffects(Player player) {
    for (MobEffectInstance effect : allEffects()) {
      MobEffectInstance current = player.getEffect(effect.getEffect());
      if (current == null || current.getDuration() < 100) {
        return false;
      }
    }
    return true;
  }

  /** Eats a single slice of cake if possible */
  private InteractionResult eatSlice(LevelAccessor world, BlockPos pos, BlockState state, Player player) {
    if (!player.canEat(false) && !food.canAlwaysEat()) {
      return InteractionResult.PASS;
    }
    // repurpose fast eating, will mean no eating if we have the effect
    if (combination == EffectCombination.BLOCK && hasAllEffects(player)) {
      return InteractionResult.PASS;
    }
    player.awardStat(Stats.EAT_CAKE_SLICE);
    // apply food stats
    player.getFoodData().eat(food);
    for (ApplyStatusEffectsConsumeEffect possible : effects) {
      if (!world.isClientSide() && world.getRandom().nextFloat() < possible.probability()) {
        for (MobEffectInstance shared : possible.effects()) {
          // effects in the component are shared, copy before applying
          MobEffectInstance effect = new MobEffectInstance(shared);
          // if adding, increase duration by current duration, provided its an exact level match
          if (combination == EffectCombination.ADD) {
            MobEffectInstance current = player.getEffect(effect.getEffect());
            if (current != null && current.getAmplifier() == effect.getAmplifier()) {
              effect = new MobEffectInstance(effect.getEffect(), effect.getDuration() + current.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
            }
          }
          player.addEffect(effect);
        }
      }
    }
    // remove one bite from the cake
    int i = state.getValue(BITES);
    if (i < 6) {
      world.setBlock(pos, state.setValue(BITES, i + 1), 3);
    } else {
      world.removeBlock(pos, false);
    }
    return InteractionResult.SUCCESS;
  }

  public enum EffectCombination {
    /** New effect will update time on existing, like potions */
    SET,
    /** New effect will increase duration of existing */
    ADD,
    /** Cake cannot be eaten if effect is present  */
    BLOCK
  }
}
