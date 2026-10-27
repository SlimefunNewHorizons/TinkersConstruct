package slimeknights.tconstruct.shared;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.List;

/** Food values. Since 1.21.2 the effects applied when eating live in a separate {@link Consumable} component. */
@SuppressWarnings("WeakerAccess")
public final class TinkerFood {
  private TinkerFood() {}
  /** Bacon. What more is there to say? */
  public static final FoodProperties BACON = (new FoodProperties.Builder()).nutrition(4).saturationModifier(0.6F).build();

  /** Cheese ingot food value. */
  public static final FoodProperties CHEESE = (new FoodProperties.Builder()).nutrition(3).saturationModifier(0.4F).build();

  /** Cheese block contains four ingots, so its food value is four times that of an ingot. */
  public static final FoodProperties CHEESE_BLOCK = (new FoodProperties.Builder()).nutrition(12).saturationModifier(0.4F).build();

  /** For the modifier */
  public static final FoodProperties JEWELED_APPLE = (new FoodProperties.Builder()).nutrition(4).saturationModifier(1.2F).alwaysEdible().build();
  public static final Consumable JEWELED_APPLE_CONSUMABLE = effects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.HASTE, 1200, 0), new MobEffectInstance(MobEffects.RESISTANCE, 2400, 0));

  /* Cake block is set up to take food as a parameter */
  public static final FoodProperties EARTH_CAKE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).alwaysEdible().build();
  public static final FoodProperties SKY_CAKE   = new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).alwaysEdible().build();
  public static final FoodProperties ICHOR_CAKE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).alwaysEdible().build();
  public static final FoodProperties ENDER_CAKE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.4f).alwaysEdible().build();
  public static final FoodProperties MAGMA_CAKE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build();
  // regen is 50 ticks per half heart, so this heals 3 per slice
  public static final FoodProperties BLOOD_CAKE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build();
  public static final Consumable EARTH_CAKE_CONSUMABLE = effects(Consumables.defaultFood(), new MobEffectInstance(TinkerEffects.bouncy.<MobEffect>getHolderAs(), 30 * 20, 0));
  public static final Consumable SKY_CAKE_CONSUMABLE   = effects(Consumables.defaultFood(), new MobEffectInstance(TinkerEffects.doubleJump.<MobEffect>getHolderAs(), 30 * 20, 0));
  public static final Consumable ICHOR_CAKE_CONSUMABLE = effects(Consumables.defaultFood(), new MobEffectInstance(TinkerEffects.antigravity.<MobEffect>getHolderAs(), 30 * 20, 0));
  public static final Consumable ENDER_CAKE_CONSUMABLE = effects(Consumables.defaultFood().consumeSeconds(0.8f), new MobEffectInstance(TinkerEffects.returning.<MobEffect>getHolderAs(), 30 * 20, 0));
  public static final Consumable MAGMA_CAKE_CONSUMABLE = effects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30 * 20, 0));
  public static final Consumable BLOOD_CAKE_CONSUMABLE = effects(Consumables.defaultFood(), new MobEffectInstance(MobEffects.REGENERATION, 3 * 50, 0));

  /** Bottles only apply effects, they have no food value */
  public static final FoodProperties BOTTLE = new FoodProperties.Builder().alwaysEdible().build();
  public static final Consumable EARTH_BOTTLE = effects(Consumables.defaultDrink(), new MobEffectInstance(TinkerEffects.experienced.<MobEffect>getHolderAs(), 120 * 20), new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 1));
  public static final Consumable SKY_BOTTLE   = effects(Consumables.defaultDrink(), new MobEffectInstance(TinkerEffects.ricochet.<MobEffect>getHolderAs(), 120 * 20), new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 1));
  public static final Consumable ICHOR_BOTTLE = effects(Consumables.defaultDrink(), new MobEffectInstance(MobEffects.LEVITATION, 10 * 20), new MobEffectInstance(MobEffects.SLOWNESS, 10 * 20, 1));
  public static final Consumable ENDER_BOTTLE = effects(Consumables.defaultDrink(), new MobEffectInstance(TinkerEffects.enderference.<MobEffect>getHolderAs(), 60 * 20), new MobEffectInstance(MobEffects.SLOWNESS, 60 * 20, 1));
  // 250 is 10 poison damage
  public static final Consumable VENOM_BOTTLE = effects(Consumables.defaultDrink(), new MobEffectInstance(MobEffects.STRENGTH, 30 * 20), new MobEffectInstance(MobEffects.POISON, 250));

  public static final FoodProperties MEAT_SOUP = new FoodProperties.Builder().nutrition(8).saturationModifier(0.6f).build();

  /** Creates a consumable that always applies the given effects */
  private static Consumable effects(Consumable.Builder builder, MobEffectInstance... effects) {
    return builder.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects))).build();
  }

  /**
   * Gets the cake for the given slime type
   * @param slime  Slime type
   * @return  Cake food
   */
  public static FoodProperties getCake(FoliageType slime) {
    return switch (slime) {
      default -> EARTH_CAKE;
      case SKY -> SKY_CAKE;
      case ICHOR -> ICHOR_CAKE;
      case BLOOD -> BLOOD_CAKE;
      case ENDER -> ENDER_CAKE;
    };
  }

  /**
   * Gets the effects of the cake for the given slime type
   * @param slime  Slime type
   * @return  Cake effects
   */
  public static Consumable getCakeConsumable(FoliageType slime) {
    return switch (slime) {
      default -> EARTH_CAKE_CONSUMABLE;
      case SKY -> SKY_CAKE_CONSUMABLE;
      case ICHOR -> ICHOR_CAKE_CONSUMABLE;
      case BLOOD -> BLOOD_CAKE_CONSUMABLE;
      case ENDER -> ENDER_CAKE_CONSUMABLE;
    };
  }

  /**
   * Gets the effects of the bottle for the given slime type
   * @param slime  Slime type
   * @return  Bottle effects
   */
  public static Consumable getBottle(SlimeType slime) {
    return switch (slime) {
      default -> EARTH_BOTTLE;
      case SKY -> SKY_BOTTLE;
      case ICHOR -> ICHOR_BOTTLE;
      case ENDER -> ENDER_BOTTLE;
    };
  }
}
