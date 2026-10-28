package slimeknights.tconstruct.library.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;

/** Slime particle using the slimeball texture, like the vanilla slime particle */
public class SlimeParticle {
  private SlimeParticle() {}

  public static class Factory extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
    private final ItemLike slime;

    public Factory(SlimeType type) {
      this(TinkerCommons.slimeball.get(type));
    }

    public Factory(ItemLike slime) {
      this.slime = slime;
    }

    @Override
    public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
      return new BreakingItemParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, getSprite(new ItemStack(slime), worldIn, random));
    }
  }
}
