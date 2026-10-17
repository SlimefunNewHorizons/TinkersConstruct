package slimeknights.tconstruct.library.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ItemStack#copy()} shares component values between the copies, while tool data is edited in place.
 * These guard against an edit to one copy leaking into the others, which corrupted the cached JEI render tools.
 */
class ItemStackUtilTest extends BaseMcTest {
  private static final String KEY = "tconstruct:test";

  @Test
  void toolCopiesDoNotShareData() {
    ItemStack original = new ItemStack(TinkerTools.pickaxe.get());
    ItemStackUtil.getOrCreateTag(original).putString(KEY, "original");

    ItemStack copy = original.copy();
    ItemStackUtil.getOrCreateTag(copy).putString(KEY, "copy");

    assertThat(ItemStackUtil.getTag(original).getString(KEY)).isEqualTo("original");
    assertThat(ItemStackUtil.getTag(copy).getString(KEY)).isEqualTo("copy");
  }

  @Test
  void partCopiesDoNotShareData() {
    ItemStack original = new ItemStack(TinkerToolParts.pickHead.get());
    ItemStackUtil.getOrCreateTag(original).putString(KEY, "original");

    ItemStack copy = original.copyWithCount(2);
    ItemStackUtil.getOrCreateTag(copy).putString(KEY, "copy");

    assertThat(ItemStackUtil.getTag(original).getString(KEY)).isEqualTo("original");
  }

  @Test
  void displayModifiersDoNotChangeTheSourceTool() {
    // JEI recipes build their display tools from one cached render tool per item
    ItemStack renderTool = new ItemStack(TinkerTools.pickaxe.get());
    ItemStackUtil.getOrCreateTag(renderTool).putBoolean(TooltipUtil.KEY_DISPLAY, true);
    CompoundTag before = ItemStackUtil.getTag(renderTool).copy();

    ItemStack display = IDisplayModifierRecipe.withModifiers(renderTool, List.of());

    assertThat(ItemStackUtil.getTag(renderTool)).isEqualTo(before);
    assertThat(ItemStackUtil.getTag(display)).isNotEqualTo(before);
  }
}
