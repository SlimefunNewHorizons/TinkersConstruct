package slimeknights.mantle.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helpers to aid in reading and writing of NBT
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TagHelper {
  /** Tag contents being loaded, recorded by {@code MappedRegistryMixin} as tags are not bound while recipe conditions run */
  private static final Map<ResourceKey<?>, Map<? extends TagKey<?>, ? extends List<? extends Holder<?>>>> PENDING_TAGS = new ConcurrentHashMap<>();

  /** Records the tags a registry is about to bind, called by {@code MappedRegistryMixin} */
  public static void setPendingTags(ResourceKey<?> registry, Map<? extends TagKey<?>, ? extends List<? extends Holder<?>>> tags) {
    PENDING_TAGS.put(registry, tags);
  }

  /** Checks if the tag is being loaded, used to resolve tags before they bind */
  public static boolean isPendingTag(TagKey<?> key) {
    Map<? extends TagKey<?>, ? extends List<? extends Holder<?>>> pending = PENDING_TAGS.get(key.registry());
    return pending != null && pending.containsKey(key);
  }

  /**
   * Gets the contents of a tag while conditions are evaluated. NeoForge only exposes {@link IContext#isTagLoaded} since 1.21.2,
   * as tags bind after recipes load, so the contents come from the tags being loaded.
   * @return  Tag contents, empty if the context cannot see tags
   */
  @SuppressWarnings("unchecked")
  public static <T> Collection<Holder<T>> getTag(IContext context, TagKey<T> key) {
    if (context instanceof DataLoadedConditionContext loaded) {
      return loaded.getTag(key);
    }
    if (!context.isTagLoaded(key)) {
      return List.of();
    }
    Map<? extends TagKey<?>, ? extends List<? extends Holder<?>>> pending = PENDING_TAGS.get(key.registry());
    if (pending != null) {
      List<? extends Holder<?>> contents = pending.get(key);
      return contents == null ? List.of() : (Collection<Holder<T>>) contents;
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
