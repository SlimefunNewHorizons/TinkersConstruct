package slimeknights.mantle.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * Bridges code written against {@link CompoundTag} to the 1.21.6 {@link ValueInput}/{@link ValueOutput} API, keeping the same saved format.
 */
public final class ValueIOHelper {
  private ValueIOHelper() {}

  /** Map codec reading or writing every key of the surrounding object as a compound */
  public static final MapCodec<CompoundTag> WHOLE_COMPOUND = new MapCodec<>() {
    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.empty();
    }

    @Override
    public <T> DataResult<CompoundTag> decode(DynamicOps<T> ops, MapLike<T> input) {
      CompoundTag tag = new CompoundTag();
      input.entries().forEach(entry -> ops.getStringValue(entry.getFirst()).result()
        .ifPresent(key -> tag.put(key, ops.convertTo(NbtOps.INSTANCE, entry.getSecond()))));
      return DataResult.success(tag);
    }

    @Override
    public <T> RecordBuilder<T> encode(CompoundTag input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      for (String key : input.keySet()) {
        Tag value = input.get(key);
        if (value != null) {
          prefix.add(key, NbtOps.INSTANCE.convertTo(ops, value));
        }
      }
      return prefix;
    }
  };

  /** Reads all values of the input as a compound */
  public static CompoundTag read(ValueInput input) {
    return input.read(WHOLE_COMPOUND).orElseGet(CompoundTag::new);
  }

  /** Writes all values of the compound to the output */
  public static void write(ValueOutput output, CompoundTag tag) {
    if (!tag.isEmpty()) {
      output.store(WHOLE_COMPOUND, tag);
    }
  }

  /** Saves a non-empty stack to NBT, replacement for the removed {@code ItemStack#save(Provider)} */
  public static Tag saveStack(ItemStack stack, HolderLookup.Provider registries) {
    return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
  }

  /** Parses a stack from NBT, returning empty if invalid. Replacement for the removed {@code ItemStack#parseOptional} */
  public static ItemStack parseStack(HolderLookup.Provider registries, Tag tag) {
    return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
  }

  /** Saves a non-empty fluid to NBT, replacement for the removed {@code FluidStack#save(Provider)} */
  public static Tag saveFluid(FluidStack fluid, HolderLookup.Provider registries) {
    return FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow();
  }

  /** Parses a fluid from NBT, returning empty if invalid. Replacement for the removed {@code FluidStack#parseOptional} */
  public static FluidStack parseFluid(HolderLookup.Provider registries, Tag tag) {
    return FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(FluidStack.EMPTY);
  }

  /** Writes the tank fluid to NBT in the format of the removed {@code FluidTank#writeToNBT} */
  public static CompoundTag writeTank(FluidTank tank, HolderLookup.Provider registries, CompoundTag nbt) {
    if (!tank.isEmpty()) {
      nbt.put("Fluid", saveFluid(tank.getFluid(), registries));
    }
    return nbt;
  }

  /** Reads the tank fluid from NBT in the format of the removed {@code FluidTank#readFromNBT} */
  public static void readTank(FluidTank tank, HolderLookup.Provider registries, CompoundTag nbt) {
    tank.setFluid(parseFluid(registries, nbt.getCompoundOrEmpty("Fluid")));
  }

  /** Saves a component to NBT, replacement for the removed {@code Component.Serializer#toJson} storage */
  public static Tag saveComponent(Component component, HolderLookup.Provider registries) {
    return ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), component).getOrThrow();
  }

  /** Parses a component from NBT, also accepting the JSON strings saved before 1.21.5 */
  @Nullable
  public static Component parseComponent(@Nullable Tag tag, HolderLookup.Provider registries) {
    if (tag == null) {
      return null;
    }
    if (tag instanceof StringTag(String value) && !value.isEmpty() && (value.charAt(0) == '{' || value.charAt(0) == '"' || value.charAt(0) == '[')) {
      try {
        return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(value)).result().orElse(null);
      } catch (RuntimeException e) {
        // not JSON, fall through to NBT
      }
    }
    return ComponentSerialization.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(null);
  }

  /** Gets registries for serializing, preferring the level's */
  public static HolderLookup.Provider registries(@Nullable Level level) {
    if (level != null) {
      return level.registryAccess();
    }
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
  }
}
