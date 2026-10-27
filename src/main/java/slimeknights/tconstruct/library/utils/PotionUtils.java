package slimeknights.tconstruct.library.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bridges the 1.20 potion NBT format used by Tinkers' Construct fluid stacks
 * and the 1.21 item data component used by vanilla potion items.
 */
public final class PotionUtils {
  public static final String TAG_POTION = "Potion";

  private PotionUtils() {}

  /** Gets the potion stored on an item using the 1.21 data component. */
  @Nullable
  public static Holder<Potion> getPotion(ItemStack stack) {
    return getPotionContents(stack).potion().orElse(null);
  }

  /** Gets the potion stored on a fluid using the 1.21 data component. */
  @Nullable
  public static Holder<Potion> getPotion(FluidStack stack) {
    return getPotionContents(stack).potion().orElse(null);
  }

  /** Gets the potion stored in Tinkers' legacy fluid NBT. */
  @Nullable
  public static Holder<Potion> getPotion(@Nullable CompoundTag tag) {
    if (tag != null && tag.contains(TAG_POTION)) {
      Identifier id = Identifier.tryParse(tag.getStringOr(TAG_POTION, ""));
      if (id != null) {
        return BuiltInRegistries.POTION.get(id).orElse(null);
      }
    }
    return null;
  }

  /** Gets the translation key for the potion contents, replacement for the removed {@code Potion#getName(Optional, String)} */
  public static String getName(PotionContents contents, String prefix) {
    return prefix + contents.potion().map(potion -> potion.value().name()).orElse("empty");
  }

  /** Gets potion contents from an item, including legacy custom data during migration. */
  public static PotionContents getPotionContents(ItemStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null ? contents : fromLegacyTag(ItemStackUtil.getTag(stack));
  }

  /** Gets potion contents from a fluid, including legacy custom data in existing recipe JSON. */
  public static PotionContents getPotionContents(FluidStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      return contents;
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return fromLegacyTag(data == null ? null : data.tag);
  }

  private static PotionContents fromLegacyTag(@Nullable CompoundTag tag) {
    Holder<Potion> potion = getPotion(tag);
    Optional<Integer> color = tag != null ? tag.getInt("CustomPotionColor") : Optional.empty();
    return potion == null && color.isEmpty()
      ? PotionContents.EMPTY
      : new PotionContents(Optional.ofNullable(potion), color, List.of(), Optional.empty());
  }

  /** Sets the vanilla 1.21 potion data component. */
  public static ItemStack setPotion(ItemStack stack, Holder<Potion> potion) {
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Sets the vanilla 1.21 potion data component on a fluid. */
  public static FluidStack setPotion(FluidStack stack, Holder<Potion> potion) {
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Copies all potion content, including custom color and effects, from an item to a fluid. */
  public static FluidStack copyPotion(ItemStack source, FluidStack target) {
    target.set(DataComponents.POTION_CONTENTS, getPotionContents(source));
    return target;
  }

  /** Copies all potion content, including custom color and effects, from a fluid to an item. */
  public static ItemStack copyPotion(FluidStack source, ItemStack target) {
    target.set(DataComponents.POTION_CONTENTS, getPotionContents(source));
    return target;
  }

  public static List<MobEffectInstance> getMobEffects(ItemStack stack) {
    PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    List<MobEffectInstance> effects = new ArrayList<>();
    contents.forEachEffect(effects::add, 1.0f);
    return effects;
  }

  public static Iterable<MobEffectInstance> getAllEffects(@Nullable CompoundTag tag) {
    Holder<Potion> potion = getPotion(tag);
    return potion == null ? List.of() : potion.value().getEffects();
  }

  public static Iterable<MobEffectInstance> getAllEffects(FluidStack stack) {
    return getPotionContents(stack).getAllEffects();
  }

  public static int getColor(ItemStack stack) {
    return getPotionContents(stack).getColor();
  }

  public static int getColor(FluidStack stack) {
    return getPotionContents(stack).getColor();
  }

  public static int getColor(Holder<Potion> potion) {
    return new PotionContents(potion).getColor();
  }

  public static int getColor(Potion potion) {
    return getColor(potion.getEffects());
  }

  public static int getColor(Iterable<MobEffectInstance> effects) {
    return PotionContents.getColorOptional(effects).orElse(PotionContents.BASE_POTION_COLOR);
  }

  public static void addPotionTooltip(ItemStack stack, List<Component> tooltip, float durationFactor) {
    PotionContents.addPotionTooltip(getPotionContents(stack).getAllEffects(), tooltip::add, durationFactor, 20.0f);
  }

  public static void addPotionTooltip(Iterable<MobEffectInstance> effects, List<Component> tooltip, float durationFactor) {
    PotionContents.addPotionTooltip(effects, tooltip::add, durationFactor, 20.0f);
  }
}
