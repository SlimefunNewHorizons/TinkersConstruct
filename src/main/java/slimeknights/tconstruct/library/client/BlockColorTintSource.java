package slimeknights.tconstruct.library.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Item tint using the block color of the item's block, for block items tinted like their block such as slime grass */
public record BlockColorTintSource(int index) implements ItemTintSource {
  public static final MapCodec<BlockColorTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    Codec.INT.optionalFieldOf("index", 0).forGetter(BlockColorTintSource::index)
  ).apply(inst, BlockColorTintSource::new));

  @Override
  public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
    if (stack.getItem() instanceof BlockItem blockItem) {
      int color = Minecraft.getInstance().getBlockColors().getColor(blockItem.getBlock().defaultBlockState(), null, null, index);
      if (color != -1) {
        return ARGB.opaque(color);
      }
    }
    return -1;
  }

  @Override
  public MapCodec<BlockColorTintSource> type() {
    return MAP_CODEC;
  }
}
