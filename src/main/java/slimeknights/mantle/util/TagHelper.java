package slimeknights.mantle.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.conditions.ConditionContext;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * Helpers to aid in reading and writing of NBT
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TagHelper {
  /** NeoForge only exposes whether a tag exists since 1.21.2, its context holds the pending tags */
  private static final Field PENDING_TAGS = ObfuscationReflectionHelper.findField(ConditionContext.class, "pendingTags");

  /**
   * Gets the contents of a tag while conditions are evaluated. NeoForge only exposes {@link IContext#isTagLoaded} since 1.21.2,
   * but its context holds the pending tags, opened by the access transformer.
   * @return  Tag contents, empty if the context cannot see tags
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> Collection<Holder<T>> getTag(IContext context, TagKey<T> key) {
    if (context instanceof ConditionContext conditions) {
      HolderLookup.RegistryLookup lookup;
      try {
        lookup = ((Map<ResourceKey<?>, HolderLookup.RegistryLookup<?>>) PENDING_TAGS.get(conditions)).get(key.registry());
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Failed to read pending tags", e);
      }
      if (lookup != null) {
        return ((Optional<HolderSet.Named<T>>) lookup.get(key)).map(set -> (Collection<Holder<T>>) set.stream().toList()).orElse(List.of());
      }
    }
    if (context instanceof DataLoadedConditionContext loaded) {
      return loaded.getTag(key);
    }
    return List.of();
  }

  /* BlockPos */

  /** @deprecated use {@link net.minecraft.nbt.NbtUtils#writeBlockPos(BlockPos)} */
  @Deprecated(forRemoval = true)
  public static CompoundTag writePos(BlockPos pos) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("X", pos.getX());
    tag.putInt("Y", pos.getY());
    tag.putInt("Z", pos.getZ());
    return tag;
  }

  /** @deprecated use {@link net.minecraft.nbt.NbtUtils#readBlockPos(CompoundTag)} */
  @Nullable
  @Deprecated(forRemoval = true)
  public static BlockPos readPos(CompoundTag tag) {
    if (tag.getDouble("X").isPresent() &&tag.getDouble("Y").isPresent() && tag.getDouble("Z").isPresent()) {
      return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
    }
    return null;
  }
}
