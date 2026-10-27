package slimeknights.tconstruct.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.tools.modifiers.effect.NoMilkEffect;

/** Keeps milk immune effects when clearing all effects, NeoForge removed effect cures in 1.21.5 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
  @Redirect(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;onEffectRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
  private boolean tconstruct$keepMilkImmune(LivingEntity living, MobEffectInstance effect) {
    // returning true cancels the removal
    return NoMilkEffect.isMilkImmune(effect) || EventHooks.onEffectRemoved(living, effect);
  }
}
