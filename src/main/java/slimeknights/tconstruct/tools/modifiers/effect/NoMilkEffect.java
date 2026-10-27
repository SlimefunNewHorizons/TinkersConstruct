package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import slimeknights.tconstruct.common.TinkerEffect;


/**
 * Effect that cannot be cured with milk
 * TODO 1.21: move to {@link slimeknights.tconstruct.shared.effect}
 */
public class NoMilkEffect extends TinkerEffect {
  public NoMilkEffect(MobEffectCategory typeIn, int color, boolean show) {
    super(typeIn, color, show);
  }

  /** Checks if milk (or anything clearing all effects) should keep the effect, replaces the effect cures NeoForge removed in 1.21.5 */
  public static boolean isMilkImmune(MobEffectInstance instance) {
    MobEffect effect = instance.getEffect().value();
    return effect instanceof NoMilkEffect || effect instanceof HelmetChargingEffect;
  }
}
