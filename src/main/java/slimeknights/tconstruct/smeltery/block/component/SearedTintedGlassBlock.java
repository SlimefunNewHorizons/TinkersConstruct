package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class SearedTintedGlassBlock extends SearedGlassBlock {
  public SearedTintedGlassBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected boolean propagatesSkylightDown(BlockState state) {
    return false;
  }

  @Override
  protected int getLightBlock(BlockState state) {
    return LightEngine.MAX_LEVEL;
  }
}
