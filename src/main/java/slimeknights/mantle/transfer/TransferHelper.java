package slimeknights.mantle.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Bridges Mantle and Tinkers' legacy {@link IFluidHandler}/{@link IItemHandler} code to the NeoForge 1.21.9+ transfer capabilities.
 * shortcut: legacy handlers are wrapped instead of ported to {@link ResourceHandler}, port them natively once the 1.21.11 port is stable.
 */
@SuppressWarnings("removal")
public final class TransferHelper {
  private TransferHelper() {}

  /* Queries */

  /** Gets the fluid handler of the block at the given position */
  @Nullable
  public static IFluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
    return unwrapFluid(level.getCapability(Capabilities.Fluid.BLOCK, pos, side));
  }

  /** Gets the item handler of the block at the given position */
  @Nullable
  public static IItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
    return unwrapItem(level.getCapability(Capabilities.Item.BLOCK, pos, side));
  }

  /** Gets the item handler exposed by the entity to automation */
  @Nullable
  public static IItemHandler getItemHandler(Entity entity, @Nullable Direction side) {
    return unwrapItem(entity.getCapability(Capabilities.Item.ENTITY_AUTOMATION, side));
  }

  /** Gets the fluid handler for an item stack, changes are applied to the stack where possible and always to {@link IFluidHandlerItem#getContainer()} */
  @Nullable
  public static IFluidHandlerItem getFluidHandler(ItemStack stack) {
    return stack.isEmpty() ? null : FluidUtil.getFluidHandler(stack).orElse(null);
  }

  /** Gets the item handler for an item stack */
  @Nullable
  public static IItemHandler getItemHandler(ItemStack stack) {
    return stack.isEmpty() ? null : unwrapItem(ItemAccess.forStack(stack).getCapability(Capabilities.Item.ITEM));
  }

  /** Converts a fluid capability to the legacy interface, unwrapping handlers wrapped by this class */
  @Nullable
  public static IFluidHandler unwrapFluid(@Nullable ResourceHandler<FluidResource> handler) {
    if (handler == null) {
      return null;
    }
    if (handler instanceof LegacyFluidResourceHandler legacy) {
      return legacy.getHandler();
    }
    return IFluidHandler.of(handler);
  }

  /** Converts an item capability to the legacy interface, unwrapping handlers wrapped by this class */
  @Nullable
  public static IItemHandler unwrapItem(@Nullable ResourceHandler<ItemResource> handler) {
    if (handler == null) {
      return null;
    }
    if (handler instanceof LegacyItemResourceHandler legacy) {
      return legacy.getHandler();
    }
    return IItemHandler.of(handler);
  }


  /* Registration */

  /** Registers a legacy fluid handler for a block entity */
  public static <BE extends BlockEntity> void registerFluid(RegisterCapabilitiesEvent event, BlockEntityType<BE> type, BiFunction<? super BE, Direction, IFluidHandler> provider) {
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> {
      IFluidHandler handler = provider.apply(be, side);
      return handler == null ? null : new LegacyFluidResourceHandler(handler);
    });
  }

  /** Registers a legacy item handler for a block entity */
  public static <BE extends BlockEntity> void registerItems(RegisterCapabilitiesEvent event, BlockEntityType<BE> type, BiFunction<? super BE, Direction, IItemHandler> provider) {
    event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> {
      IItemHandler handler = provider.apply(be, side);
      return handler == null ? null : new LegacyItemResourceHandler(handler);
    });
  }

  /** Registers a legacy fluid handler for items, the factory receives a single item copy and must expose the result through {@link IFluidHandlerItem#getContainer()} */
  public static void registerItemFluid(RegisterCapabilitiesEvent event, Function<ItemStack, ? extends IFluidHandlerItem> factory, ItemLike... items) {
    // legacy factories return null for items lacking the capability
    event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> factory.apply(stack.copy()) == null ? null : new LegacyFluidItemResourceHandler(access, factory), items);
  }

  /** Registers a legacy item handler for items, the factory receives a single item copy and must store changes in that stack */
  public static void registerItemItems(RegisterCapabilitiesEvent event, Function<ItemStack, ? extends IItemHandler> factory, ItemLike... items) {
    event.registerItem(Capabilities.Item.ITEM, (stack, access) -> factory.apply(stack.copy()) == null ? null : new LegacyItemItemResourceHandler(access, factory), items);
  }

  /** Registers a legacy energy storage for items, the factory receives a single item copy and must store changes in that stack */
  public static void registerItemEnergy(RegisterCapabilitiesEvent event, Function<ItemStack, ? extends IEnergyStorage> factory, ItemLike... items) {
    event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> factory.apply(stack.copy()) == null ? null : new LegacyEnergyItemHandler(access, factory), items);
  }
}
