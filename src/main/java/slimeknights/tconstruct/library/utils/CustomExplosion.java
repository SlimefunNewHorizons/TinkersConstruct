package slimeknights.tconstruct.library.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Helper class for more control over explosions.
 * Since 1.21.2 explosions only run on the server, the client is informed through {@link ClientboundExplodePacket}.
 */
public class CustomExplosion extends ServerExplosion {
  /** Size of the hollowed out cube determining the number of rays to cast */
  private static final int RAY_COUNT = 16;
  private static final int MAX_RAY = RAY_COUNT - 1;
  /** Default predicate for which entities to match */
  public static final Predicate<Entity> DEFAULT_ENTITY_PREDICATE = entity -> entity != null && entity.isAlive() && !entity.isSpectator();
  /** Same block particles as {@link ServerLevel#explode(Entity, double, double, double, float, Level.ExplosionInteraction)} */
  private static final WeightedList<ExplosionParticleInfo> BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
    .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
    .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
    .build();

  /* ServerExplosion keeps its state private, so keep the parts needed by the custom raycast and damage algorithms */
  protected final ServerLevel level;
  protected final double x;
  protected final double y;
  protected final double z;
  protected final float radius;
  @Nullable
  protected final Entity source;
  protected final DamageSource damageSource;
  protected final ExplosionDamageCalculator damageCalculator;
  protected final boolean fire;
  protected final BlockInteraction blockInteraction;
  protected final List<BlockPos> toBlow = new ArrayList<>();
  protected final Map<Player,Vec3> hitPlayers;

  /** Maximum damage to deal; setting to 7*2*radius will match the vanilla explosion. */
  protected final float damage;
  /** Entity knockback scale. May be 0 to prevent knockback or negative to reverse knockback */
  protected final float knockback;
  /** Determines which entities are affected by this explosion */
  protected final Predicate<Entity> entityPredicate;
  /** If true, explosion damage bypasses the invulnerability time */
  protected final boolean bypassInvulnerableTime;

  /** @param level  Level for the explosion, must be a server level */
  public CustomExplosion(Level level, Vec3 location, float radius, @Nullable Entity sourceEntity, @Nullable Predicate<Entity> entityPredicate, float damage, @Nullable DamageSource damageSource, float knockback, @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire, BlockInteraction blockInteraction, boolean bypassInvulnerableTime) {
    super((ServerLevel) level, sourceEntity, damageSource, damageCalculator, location, radius, placeFire, blockInteraction);
    this.level = (ServerLevel) level;
    this.x = location.x;
    this.y = location.y;
    this.z = location.z;
    this.radius = radius;
    this.source = sourceEntity;
    this.damageSource = getDamageSource();
    this.damageCalculator = damageCalculator == null
      ? (sourceEntity == null ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(sourceEntity))
      : damageCalculator;
    this.fire = placeFire;
    this.blockInteraction = blockInteraction;
    this.hitPlayers = getHitPlayers();
    this.entityPredicate = Objects.requireNonNullElse(entityPredicate, DEFAULT_ENTITY_PREDICATE);
    this.damage = damage;
    this.knockback = knockback;
    this.bypassInvulnerableTime = bypassInvulnerableTime;
  }

  public CustomExplosion(Level level, Vec3 location, float radius, @Nullable Entity sourceEntity, @Nullable Predicate<Entity> entityPredicate, float damage, @Nullable DamageSource damageSource, float knockback, @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire, BlockInteraction blockInteraction) {
    this(level, location, radius, sourceEntity, entityPredicate ,damage, damageSource, knockback, damageCalculator, placeFire, blockInteraction, false);
  }

  @Override
  public int explode() {
    this.level.gameEvent(this.source, GameEvent.EXPLODE, center());
    calculateHitBlocks();
    damageAndPushEntities();
    if (interactsWithBlocks()) {
      interactWithBlocks();
    }
    if (fire) {
      createFire();
    }
    return toBlow.size();
  }

  /** Compatibility name for the old Explosion position accessor. */
  protected Vec3 getPosition() {
    return center();
  }

  /** Checks if this explosion breaks blocks */
  protected boolean interactsWithBlocks() {
    return blockInteraction != BlockInteraction.KEEP;
  }

  /** Calculates the list of blocks to hit, filling {@link #toBlow} */
  protected void calculateHitBlocks() {
    // optimization: if we are not interacting with blocks, no need to calculate blocks
    if (!interactsWithBlocks() && !fire) {
      return;
    }

    Set<BlockPos> set = new HashSet<>();
    // loop over a hollowed out 16x cube
    for (int rayX = 0; rayX < RAY_COUNT; rayX++) {
      for (int rayY = 0; rayY < RAY_COUNT; rayY++) {
        for (int rayZ = 0; rayZ < RAY_COUNT; rayZ++) {
          if (rayX == 0 || rayX == MAX_RAY || rayY == 0 || rayY == MAX_RAY || rayZ == 0 || rayZ == MAX_RAY) {
            // determine direction to go, then step in 0.3 unit vector increments
            double stepX = rayX * 2.0 / MAX_RAY - 1;
            double stepY = rayY * 2.0 / MAX_RAY - 1;
            double stepZ = rayZ * 2.0 / MAX_RAY - 1;
            double stepScale = 0.3f / Math.sqrt(stepX * stepX + stepY * stepY + stepZ * stepZ);
            stepX *= stepScale;
            stepY *= stepScale;
            stepZ *= stepScale;

            // keep moving in the direction of the ray until we run out of power; means blocks with high blast resistance shield those with less
            double targetX = this.x;
            double targetY = this.y;
            double targetZ = this.z;
            for (float power = this.radius * (0.7f + level.random.nextFloat() * 0.6f); power > 0; power -= 0.225f) {
              BlockPos target = BlockPos.containing(targetX, targetY, targetZ);
              BlockState block = level.getBlockState(target);
              FluidState fluid = level.getFluidState(target);
              if (!level.isInWorldBounds(target)) {
                break;
              }

              // reduce power based on blast resistance
              Optional<Float> resistance = damageCalculator.getBlockExplosionResistance(this, level, target, block, fluid);
              if (resistance.isPresent()) {
                power -= (resistance.get() + 0.3f) * 0.3f;
              }

              // remove block if power is high enough
              // optimization: skip air if not placing fires to save network traffic
              if ((fire || !block.isAir()) && power > 0 && damageCalculator.shouldBlockExplode(this, level, target, block, power)) {
                set.add(target);
              }

              // vanilla difference - we moved the 0.3 multiplier to the original step variables to avoid needing to compute as often
              targetX += stepX;
              targetY += stepY;
              targetZ += stepZ;
            }
          }
        }
      }
    }
    toBlow.addAll(set);
  }

  /** Called to run the logic for damaging and blasting back entities in range */
  protected void damageAndPushEntities() {
    // skip running if we disabled both damage and knockback as there is nothing left to do
    if (damage <= 0 && knockback == 0) {
      return;
    }

    float diameter = this.radius * 2;
    // small behavior change: we filter the list of entities on fetch, meaning the forge event gets the filtered list
    List<Entity> list = this.level.getEntities(
      this.source,
      new AABB(Math.floor(this.x - diameter - 1),
               Math.floor(this.y - diameter - 1),
               Math.floor(this.z - diameter - 1),
               Math.floor(this.x + diameter + 1),
               Math.floor(this.y + diameter + 1),
               Math.floor(this.z + diameter + 1)),
      entityPredicate);
    EventHooks.onExplosionDetonate(this.level, this, list, toBlow);

    // start pushing entities
    // this logic is for the most part identical to vanilla, except taking better advantage of vec3
    Vec3 center = getPosition();
    for (Entity entity : list) {
      if (entity.ignoreExplosion(this)) {
        continue;
      }
      Vec3 dir = entity.position().subtract(center);
      double length = dir.length();
      double distance = length / diameter;
      if (distance <= 1) {
        // non-TNT uses eye height for explosion direction
        if (!(entity instanceof PrimedTnt)) {
          dir = dir.add(0, entity.getEyeY() - entity.getY(), 0);
          length = dir.length();
        }
        // vanilla change: a bit of tolerance on the length check to match normalize
        if (length > 1.0E-4D) {
          double strength = (1 - distance) * getSeenPercent(center, entity);
          // vanilla change: instead of multiplying the damage by 7, we make that a parameter, which can be 0 for no damage
          if (damage > 0) {
            int toDeal = (int) ((strength * strength + strength) / 2 * damage + 1);
            if (bypassInvulnerableTime) {
              ToolAttackUtil.hurtNoInvulnerableTime(entity, damageSource, toDeal);
            } else {
              entity.hurtServer(level, damageSource, toDeal);
            }
          }

          // apply enchantment to reduce knockback
          if (knockback != 0) {
            double adjustedStrength = strength * knockback;
            if (entity instanceof LivingEntity living) {
              adjustedStrength *= 1.0 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            }
            Vec3 velocity = dir.scale(adjustedStrength / length);
            velocity = EventHooks.getExplosionKnockback(level, this, entity, velocity, toBlow);
            entity.push(velocity);
            if (entity instanceof Player player) {
              if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                hitPlayers.put(player, velocity);
              }
            }
          }
          entity.onExplosionHit(source);
        }
      }
    }
  }

  /** Breaks all blocks in {@link #toBlow}, based on {@link ServerExplosion#explode()} */
  protected void interactWithBlocks() {
    List<StackDrop> drops = new ArrayList<>();
    Util.shuffle(toBlow, level.random);
    for (BlockPos pos : toBlow) {
      level.getBlockState(pos).onExplosionHit(level, pos, this, (stack, dropPos) -> {
        for (StackDrop drop : drops) {
          if (ItemEntity.areMergable(drop.stack, stack)) {
            drop.stack = ItemEntity.merge(drop.stack, stack, 16);
            if (stack.isEmpty()) {
              return;
            }
          }
        }
        drops.add(new StackDrop(dropPos, stack));
      });
    }
    for (StackDrop drop : drops) {
      Block.popResource(level, drop.pos, drop.stack);
    }
  }

  /** Places fire on some of the hit blocks, based on {@link ServerExplosion#explode()} */
  protected void createFire() {
    for (BlockPos pos : toBlow) {
      if (level.random.nextInt(3) == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender()) {
        level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
      }
    }
  }

  /** Runs the logic on the server, syncing to the client. Based on {@link ServerLevel#explode(Entity, double, double, double, float, Level.ExplosionInteraction)} */
  public void handleServer() {
    if (!EventHooks.onExplosionStart(level, this)) {
      syncToClient(explode());
    }
  }

  /** @deprecated explosions only run on the server since 1.21.2, use {@link #handleServer()} */
  @Deprecated
  public void doDualSide(Level level, boolean spawnParticles) {
    handleServer();
  }

  /** Syncs this explosion to the client */
  public void syncToClient(int blockCount) {
    Vec3 position = getPosition();
    for (ServerPlayer player : level.players()) {
      if (player.distanceToSqr(position) < 4096.0D) {
        player.connection.send(new ClientboundExplodePacket(position, radius, blockCount, Optional.ofNullable(hitPlayers.get(player)),
          isSmall() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE, BLOCK_PARTICLES));
      }
    }
  }

  /** Mutable drop entry for merging stacks, mirrors the private {@code ServerExplosion.StackCollector} */
  private static class StackDrop {
    final BlockPos pos;
    ItemStack stack;

    StackDrop(BlockPos pos, ItemStack stack) {
      this.pos = pos;
      this.stack = stack;
    }
  }
}
