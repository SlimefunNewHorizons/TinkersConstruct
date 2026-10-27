package slimeknights.tconstruct.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

/** Head item for enderman head, which counts as a pumpkin on the head */
public class EndermanHeadItem extends StandingAndWallBlockItem {
  public EndermanHeadItem(Block pBlock, Block pWallBlock, Direction pAttachmentDirection, Properties pProperties) {
    super(pBlock, pWallBlock, pAttachmentDirection, pProperties);
  }

  @Override
  public boolean isGazeDisguise(ItemStack stack, Player player, @Nullable LivingEntity entity) {
    return true;
  }
}
