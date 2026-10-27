package slimeknights.tconstruct.library.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/** Helpers related to Tag */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
  /** Type for {@link #contains(CompoundTag, String, int)} matching any number, vanilla removed its constant in 1.21.5 */
  public static final int TAG_ANY_NUMERIC = 99;

  /** Checks if the tag has the key with the given type, replacement for the removed {@code CompoundTag#contains(String, int)} */
  public static boolean contains(CompoundTag tag, String key, int type) {
    Tag value = tag.get(key);
    return value != null && (value.getId() == type || type == TAG_ANY_NUMERIC && value instanceof NumericTag);
  }

  /* Helper functions */

  /**
   * Reads a block position from Tag
   * @param parent  Parent tag
   * @param key     Position key
   * @param offset  Amount to offset position by
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readOptionalPos(CompoundTag parent, String key, BlockPos offset) {
    if (parent.getCompound(key).isPresent()) {
      CompoundTag legacy = parent.getCompoundOrEmpty(key);
      return new BlockPos(legacy.getIntOr("X", 0), legacy.getIntOr("Y", 0), legacy.getIntOr("Z", 0)).offset(offset);
    }
    return parent.read(key, BlockPos.CODEC).map(pos -> pos.offset(offset)).orElse(null);
  }

  /**
   * Checks if the given tag is a numeric type
   * @param tag  Tag to check
   * @return  True if the type matches
   */
  public static boolean isNumeric(Tag tag) {
    byte type = tag.getId();
    return type == Tag.TAG_BYTE || type == Tag.TAG_SHORT || type == Tag.TAG_INT || type == Tag.TAG_LONG || type == Tag.TAG_FLOAT || type == Tag.TAG_DOUBLE;
  }
}
