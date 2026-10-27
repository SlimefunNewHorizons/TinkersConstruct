"""NeoForge 21.5 removed EffectCure. Milk immunity moves to NoMilkEffect#isMilkImmune plus a removeAllEffects mixin."""
import re

from portio import edit, read, write

T = 'src/main/java/slimeknights/tconstruct/'
IMMUNE = 'slimeknights.tconstruct.tools.modifiers.effect.NoMilkEffect.isMilkImmune'
INSTANCE_SHORTCUT = '// shortcut: NeoForge removed per instance effect cures in 1.21.5, so milk cures this effect; restore once a cure API returns'

edit(T + 'tools/modifiers/effect/NoMilkEffect.java', [
  ('''  @Override
  public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    cures.clear();
  }''', '''  /** Checks if milk (or anything clearing all effects) should keep the effect, replaces the effect cures NeoForge removed in 1.21.5 */
  public static boolean isMilkImmune(MobEffectInstance instance) {
    MobEffect effect = instance.getEffect().value();
    return effect instanceof NoMilkEffect || effect instanceof HelmetChargingEffect;
  }'''),
  ('import net.neoforged.neoforge.common.EffectCure;\n', 'import net.minecraft.world.effect.MobEffect;\n'),
  ('\nimport java.util.Set;\n', '\n'),
])
edit(T + 'tools/modifiers/effect/HelmetChargingEffect.java', [
  ('''  @Override
  public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
  }

''', ''),
])
text = read(T + 'tools/modifiers/effect/HelmetChargingEffect.java')
text = re.sub(r'import net\.neoforged\.neoforge\.common\.EffectCure;\r?\n', '', text)
write(T + 'tools/modifiers/effect/HelmetChargingEffect.java', text)

edit(T + 'fluids/item/PotionBucketItem.java', [('''          newEffect.getCures().clear();
          newEffect.getCures().addAll(effect.getCures());
''', '')])
edit(T + 'library/modifiers/fluid/FluidMobEffect.java', [
  ('''    if (curativeItems != null) {
      instance.getCures().clear();
      curativeItems.stream().map(FluidMobEffect::cureForItem).forEach(instance.getCures()::add);
    }
''', '    ' + INSTANCE_SHORTCUT + '\n'),
  ('''  /** Maps legacy item-based cures to stable NeoForge cure identifiers. */
  public static net.neoforged.neoforge.common.EffectCure cureForItem(Item item) {
    return net.neoforged.neoforge.common.EffectCure.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
  }

''', ''),
])
edit(T + 'library/modifiers/modules/combat/MobEffectModule.java', [('''        if (curativeItems != null) {
          instance.getCures().clear();
          curativeItems.stream().map(FluidMobEffect::cureForItem).forEach(instance.getCures()::add);
        }
''', '        ' + INSTANCE_SHORTCUT + '\n')])
edit(T + 'tools/modifiers/traits/skull/RevengeModifier.java', [('''      effect.getCures().clear();
      effect.getCures().add(FluidMobEffect.cureForItem(living.getItemBySlot(slotType).getItem()));
''', '      ' + INSTANCE_SHORTCUT + '\n')])
edit(T + 'tools/modifiers/traits/skull/StrongBonesModifier.java', [('''      effect.getCures().clear();
      effect.getCures().add(FluidMobEffect.cureForItem(helmet.getItem()));
''', '      ' + INSTANCE_SHORTCUT + '\n')])
edit(T + 'library/modifiers/modules/interaction/edible/EdibleCureEffectsModule.java', [('instance.getCures().contains(net.neoforged.neoforge.common.EffectCures.MILK)', '!' + IMMUNE + '(instance)')])
edit(T + 'shared/item/CheeseItem.java', [('effect.getCures().contains(EffectCures.MILK)', '!' + IMMUNE + '(effect)')])
edit(T + 'tools/logic/ModifierEvents.java', [('!newEffect.getCures().isEmpty()', '!' + IMMUNE + '(newEffect)')])
edit(T + 'tools/modifiers/traits/skull/PlagueModifier.java', [('!effect.getCures().isEmpty()', '!' + IMMUNE + '(effect)')])
edit(T + 'tools/modules/ReduceEffectOnUnequipModule.java', [('!instance.getCures().isEmpty()', '!' + IMMUNE + '(instance)')])
edit(T + 'library/modifiers/fluid/entity/CureEffectsFluidEffect.java', [('''      // when simulating, search the effects list directly for curative effects
      // may still be wrong if the event cancels things though, no way to safely simulate it
      if (action.simulate()) {
        var cure = slimeknights.tconstruct.library.modifiers.fluid.FluidMobEffect.cureForItem(stack.getItem());
        return target.getActiveEffects().stream().anyMatch(effect -> effect.getCures().contains(cure)) ? 1 : 0;
      }
      var cure = slimeknights.tconstruct.library.modifiers.fluid.FluidMobEffect.cureForItem(stack.getItem());
      boolean removed = target.removeEffectsCuredBy(cure);
      if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
        removed |= target.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
      } else if (stack.is(net.minecraft.world.item.Items.HONEY_BOTTLE)) {
        removed |= target.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.HONEY);
      }
      return removed ? 1 : 0;''', '''      // NeoForge removed effect cures in 1.21.5, so only the vanilla milk and honey cures remain
      boolean milk = stack.is(net.minecraft.world.item.Items.MILK_BUCKET);
      boolean honey = stack.is(net.minecraft.world.item.Items.HONEY_BOTTLE);
      // when simulating, search the effects list directly for curative effects
      // may still be wrong if the event cancels things though, no way to safely simulate it
      if (action.simulate()) {
        return target.getActiveEffects().stream().anyMatch(effect -> milk ? !''' + IMMUNE + '''(effect) : honey && effect.is(net.minecraft.world.effect.MobEffects.POISON)) ? 1 : 0;
      }
      boolean removed = milk ? target.removeAllEffects() : honey && target.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
      return removed ? 1 : 0;''')])

write(T + 'mixin/LivingEntityMixin.java', '''package slimeknights.tconstruct.mixin;

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
''')
edit('src/main/resources/tconstruct.mixins.json', [('    "ItemStackMixin"\n', '    "ItemStackMixin",\n    "LivingEntityMixin"\n')])
print('ok')
