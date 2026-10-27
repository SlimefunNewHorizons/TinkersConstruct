package slimeknights.mantle.fluid;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.Map;

/** Fluid where up is down and down is up */
public abstract class InvertedFluid extends BaseFlowingFluid {
  protected InvertedFluid(Properties properties) {
    super(properties);
  }

  private boolean affectsFlow(FluidState state) {
    return state.isEmpty() || state.getType().isSame(this);
  }

  private void spreadToSides(ServerLevel level, BlockPos pos, FluidState fluid, BlockState block) {
    int amount = fluid.getAmount() - this.getDropOff(level);
    if (fluid.getValue(FALLING)) {
      amount = 7;
    }
    if (amount > 0) {
      for (Map.Entry<Direction,FluidState> entry : this.getSpread(level, pos, block).entrySet()) {
        Direction direction = entry.getKey();
        BlockPos side = pos.relative(direction);
        this.spreadTo(level, side, level.getBlockState(side), direction, entry.getValue());
      }
    }
  }

  private static boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState) {
    return !Shapes.mergedFaceOccludes(state.getCollisionShape(level, pos), spreadState.getCollisionShape(level, spreadPos), direction);
  }

  private static short getCacheKey(BlockPos sourcePos, BlockPos spreadPos) {
    int x = spreadPos.getX() - sourcePos.getX();
    int z = spreadPos.getZ() - sourcePos.getZ();
    return (short)((x + 128 & 0xFF) << 8 | z + 128 & 0xFF);
  }

  private boolean canMaybePassThrough(BlockGetter level, BlockPos pos, BlockState state, Direction direction, BlockPos spreadPos, BlockState spreadState, FluidState fluidState) {
    return !this.isSourceBlockOfThisType(fluidState) && canHoldAnyFluid(spreadState) && canPassThroughWall(direction, level, pos, state, spreadPos, spreadState);
  }

  private boolean canPassThrough(BlockGetter level, Fluid fluid, BlockPos pos, BlockState state, Direction direction, BlockPos spreadPos, BlockState spreadState, FluidState fluidState) {
    return this.canMaybePassThrough(level, pos, state, direction, spreadPos, spreadState, fluidState) && canHoldSpecificFluid(level, spreadPos, spreadState, fluid);
  }

  private boolean isSourceBlockOfThisType(FluidState state) {
    return state.getType().isSame(this) && state.isSource();
  }

  private int sourceNeighborCount(LevelReader level, BlockPos pos) {
    int count = 0;
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      if (this.isSourceBlockOfThisType(level.getFluidState(pos.relative(direction)))) {
        count++;
      }
    }
    return count;
  }

  private static boolean canHoldAnyFluid(BlockState state) {
    Block block = state.getBlock();
    if (block instanceof LiquidBlockContainer) {
      return true;
    }
    if (state.blocksMotion() || block instanceof DoorBlock || state.is(BlockTags.SIGNS) || state.is(Blocks.LADDER) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BUBBLE_COLUMN)) {
      return false;
    }
    return !state.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.END_PORTAL) && !state.is(Blocks.END_GATEWAY) && !state.is(Blocks.STRUCTURE_VOID);
  }

  private static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    return !(state.getBlock() instanceof LiquidBlockContainer container) || container.canPlaceLiquid(null, level, pos, state, fluid);
  }

  @Override
  public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluid) {
    double xHeight = 0.0D;
    double zHeight = 0.0D;
    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

    for (Direction direction : Direction.Plane.HORIZONTAL) {
      mutable.setWithOffset(pos, direction);
      FluidState sideFluid = level.getFluidState(mutable);
      if (this.affectsFlow(sideFluid)) {
        float sideHeight = sideFluid.getOwnHeight();
        float deltaHeight = 0.0F;
        if (sideHeight == 0.0F) {
          if (!level.getBlockState(mutable).blocksMotion()) {
            BlockPos above = mutable.above();
            FluidState aboveFluid = level.getFluidState(above);
            if (this.affectsFlow(aboveFluid)) {
              sideHeight = aboveFluid.getOwnHeight();
              if (sideHeight > 0.0F) {
                deltaHeight = fluid.getOwnHeight() - sideHeight + 0.8888889F;
              }
            }
          }
        } else if (sideHeight > 0.0F) {
          deltaHeight = fluid.getOwnHeight() - sideHeight;
        }

        if (deltaHeight != 0.0F) {
          xHeight += direction.getStepX() * deltaHeight;
          zHeight += direction.getStepZ() * deltaHeight;
        }
      }
    }

    Vec3 vector = new Vec3(xHeight, 0.0D, zHeight);
    if (fluid.getValue(FALLING)) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        mutable.setWithOffset(pos, direction);
        if (this.isSolidFace(level, mutable, direction) || this.isSolidFace(level, mutable.below(), direction)) {
          vector = vector.normalize().add(0.0D, 6.0D, 0.0D);
          break;
        }
      }
    }

    return vector.normalize();
  }

  @Override
  protected boolean isSolidFace(BlockGetter level, BlockPos neighbor, Direction side) {
    BlockState block = level.getBlockState(neighbor);
    FluidState fluid = level.getFluidState(neighbor);
    return !fluid.getType().isSame(this) && (side == Direction.DOWN || !(block.getBlock() instanceof IceBlock) && block.isFaceSturdy(level, neighbor, side));
  }

  @Override
  protected void spread(ServerLevel level, BlockPos pos, BlockState block, FluidState fluid) {
    // recreation that swaps downs for ups
    if (!fluid.isEmpty()) {
      BlockPos above = pos.above();
      BlockState aboveBlock = level.getBlockState(above);
      FluidState aboveFluid = aboveBlock.getFluidState();
      if (this.canMaybePassThrough(level, pos, block, Direction.UP, above, aboveBlock, aboveFluid)) {
        FluidState newFluid = this.getNewLiquid(level, above, aboveBlock);
        Fluid newType = newFluid.getType();
        if (aboveFluid.canBeReplacedWith(level, above, newType, Direction.UP) && canHoldSpecificFluid(level, above, aboveBlock, newType)) {
          this.spreadTo(level, above, aboveBlock, Direction.UP, newFluid);
          if (this.sourceNeighborCount(level, pos) >= 3) {
            this.spreadToSides(level, pos, fluid, block);
          }
          return;
        }
      }
      if (fluid.isSource() || !this.isWaterHole(level, pos, block, above, aboveBlock)) {
        this.spreadToSides(level, pos, fluid, block);
      }
    }
  }

  @Override
  protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState block) {
    int maxSide = 0;
    int sourceSides = 0;

    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos side = pos.relative(direction);
      BlockState sideBlock = level.getBlockState(side);
      FluidState sideFluid = sideBlock.getFluidState();
      if (sideFluid.getType().isSame(this) && canPassThroughWall(direction, level, pos, block, side, sideBlock)) {
        if (sideFluid.isSource() && EventHooks.canCreateFluidSource(level, side, sideBlock)) {
          sourceSides++;
        }
        maxSide = Math.max(maxSide, sideFluid.getAmount());
      }
    }

    if (sourceSides >= 2) {
      BlockState aboveBlock = level.getBlockState(pos.above());
      FluidState aboveFluid = aboveBlock.getFluidState();
      if (aboveBlock.isSolid() || this.isSourceBlockOfThisType(aboveFluid)) {
        return this.getSource(false);
      }
    }

    BlockPos below = pos.below();
    BlockState belowBlock = level.getBlockState(below);
    FluidState belowFluid = belowBlock.getFluidState();
    if (!belowFluid.isEmpty() && belowFluid.getType().isSame(this) && canPassThroughWall(Direction.DOWN, level, pos, block, below, belowBlock)) {
      return this.getFlowing(8, true);
    }
    int newHeight = maxSide - this.getDropOff(level);
    return newHeight <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(newHeight, false);
  }

  /** Checks if the fluid can flow up into the given position, the inverted version of vanilla's water hole */
  protected boolean isWaterHole(BlockGetter level, BlockPos pos, BlockState block, BlockPos spreadPos, BlockState spreadBlock) {
    return canPassThroughWall(Direction.UP, level, pos, block, spreadPos, spreadBlock)
      && (spreadBlock.getFluidState().getType().isSame(this) || canHoldAnyFluid(spreadBlock) && canHoldSpecificFluid(level, spreadPos, spreadBlock, this.getFlowing()));
  }

  /** Caches block states and holes around the spreading position, inverted version of vanilla's spread context */
  private class SpreadCache {
    private final BlockGetter level;
    private final BlockPos origin;
    private final Short2ObjectMap<BlockState> stateCache = new Short2ObjectOpenHashMap<>();
    private final Short2BooleanMap holeCache = new Short2BooleanOpenHashMap();

    private SpreadCache(BlockGetter level, BlockPos origin) {
      this.level = level;
      this.origin = origin;
    }

    private BlockState getBlockState(BlockPos pos) {
      return stateCache.computeIfAbsent(getCacheKey(origin, pos), k -> level.getBlockState(pos));
    }

    private boolean isHole(BlockPos pos) {
      return holeCache.computeIfAbsent(getCacheKey(origin, pos), k -> {
        BlockPos above = pos.above();
        return isWaterHole(level, pos, getBlockState(pos), above, level.getBlockState(above));
      });
    }
  }

  private int getSlopeDistance(LevelReader level, BlockPos pos, int depth, Direction from, BlockState block, SpreadCache cache) {
    int minSlope = 1000;
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      if (direction != from) {
        BlockPos side = pos.relative(direction);
        BlockState sideBlock = cache.getBlockState(side);
        if (this.canPassThrough(level, this.getFlowing(), pos, block, direction, side, sideBlock, sideBlock.getFluidState())) {
          if (cache.isHole(side)) {
            return depth;
          }
          if (depth < this.getSlopeFindDistance(level)) {
            int slopeDistance = this.getSlopeDistance(level, side, depth + 1, direction.getOpposite(), sideBlock, cache);
            if (slopeDistance < minSlope) {
              minSlope = slopeDistance;
            }
          }
        }
      }
    }
    return minSlope;
  }

  @Override
  protected Map<Direction, FluidState> getSpread(ServerLevel level, BlockPos pos, BlockState block) {
    int minDistance = 1000;
    Map<Direction, FluidState> spread = Maps.newEnumMap(Direction.class);
    SpreadCache cache = null;

    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos side = pos.relative(direction);
      BlockState sideBlock = level.getBlockState(side);
      FluidState sideFluid = sideBlock.getFluidState();
      if (this.canMaybePassThrough(level, pos, block, direction, side, sideBlock, sideFluid)) {
        FluidState newFluid = this.getNewLiquid(level, side, sideBlock);
        if (canHoldSpecificFluid(level, side, sideBlock, newFluid.getType())) {
          if (cache == null) {
            cache = new SpreadCache(level, pos);
          }
          int distance = cache.isHole(side) ? 0 : this.getSlopeDistance(level, side, 1, direction.getOpposite(), sideBlock, cache);
          if (distance < minDistance) {
            spread.clear();
          }
          if (distance <= minDistance) {
            if (sideFluid.canBeReplacedWith(level, side, newFluid.getType(), direction)) {
              spread.put(direction, newFluid);
            }
            minDistance = distance;
          }
        }
      }
    }

    return spread;
  }

  public static class Flowing extends InvertedFluid {
    public Flowing(Properties properties) {
      super(properties);
      this.registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 7));
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
      super.createFluidStateDefinition(builder);
      builder.add(LEVEL);
    }

    @Override
    public int getAmount(FluidState state) {
      return state.getValue(LEVEL);
    }

    @Override
    public boolean isSource(FluidState state) {
      return false;
    }
  }

  public static class Source extends InvertedFluid {
    public Source(Properties properties) {
      super(properties);
    }

    @Override
    public int getAmount(FluidState state) {
      return 8;
    }

    @Override
    public boolean isSource(FluidState state) {
      return true;
    }
  }
}
