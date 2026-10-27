package slimeknights.tconstruct.library.recipe.ingredient;

import slimeknights.mantle.util.ValueIOHelper;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.JsonCodec;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/** Ingredient matching an {@link net.minecraft.world.item.InstrumentItem} with a particular instrument. */
public class InstrumentIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("instrument");

  private final Item item;
  @Nullable
  private final ResourceKey<Instrument> instrument;
  @Nullable
  private final TagKey<Instrument> ignore;

  protected InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) {
    this.item = item.asItem();
    this.instrument = instrument;
    this.ignore = ignore;
  }

  /** Creates a new instance matching the given instrument */
  public static Ingredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), instrument, null).toVanilla();
  }

  /** Creates a new instance ignoring the given tag */
  public static Ingredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, ignore).toVanilla();
  }

  /** Gets the item matched by this ingredient */
  public Item getItem() {
    return item;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !stack.is(item)) {
      return false;
    }
    // 1.21 moved the instrument from NBT to a data component
    InstrumentComponent component = stack.get(DataComponents.INSTRUMENT);
    if (component != null) {
      if (this.instrument != null) {
        return component.instrument().key().map(key -> key.equals(this.instrument)).orElse(false);
      }
      assert this.ignore != null;
      // must be a valid instrument outside the ignored tag
      return component.unwrap(ValueIOHelper.registries(null)).map(holder -> !holder.is(this.ignore)).orElse(false);
    }
    // if no instrument, its fine as long as we don't have a specific instrument
    return this.instrument == null;
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public Stream<Holder<Item>> items() {
    return getItems().map(ItemStack::getItemHolder).distinct();
  }

  @Override
  public SlotDisplay display() {
    return new SlotDisplay.Composite(getItems().<SlotDisplay>map(SlotDisplay.ItemStackSlotDisplay::new).toList());
  }

  /** Gets the stacks to display for this ingredient */
  public Stream<ItemStack> getItems() {
    ItemStack stack = new ItemStack(item);
    // set the instrument on the stack so the display shows the right item
    if (instrument != null) {
      stack.set(DataComponents.INSTRUMENT, new InstrumentComponent(instrument));
    }
    return Stream.of(stack);
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  /** Codec for parsing this ingredient from JSON */
  private static final JsonCodec<InstrumentIngredient> CODEC_BODY = new JsonCodec<>() {
    @Override
    public InstrumentIngredient deserialize(JsonElement element, DynamicOps<?> ops) {
      JsonObject json = element.getAsJsonObject();
      Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(GsonHelper.getAsString(json, "item")));
      ResourceKey<Instrument> instrument = null;
      TagKey<Instrument> ignore = null;
      if (json.has("instrument")) {
        instrument = ResourceKey.create(Registries.INSTRUMENT, Identifier.parse(GsonHelper.getAsString(json, "instrument")));
      } else if (json.has("ignore")) {
        ignore = TagKey.create(Registries.INSTRUMENT, Identifier.parse(GsonHelper.getAsString(json, "ignore")));
      } else {
        throw new JsonSyntaxException("Invalid InstrumentIngredient: must set either 'instrument' or 'ignore'");
      }
      return new InstrumentIngredient(item, instrument, ignore);
    }

    @Override
    public JsonElement serialize(InstrumentIngredient ingredient, DynamicOps<?> ops) {
      JsonObject json = new JsonObject();
      json.addProperty("type", ID.toString());
      json.addProperty("item", BuiltInRegistries.ITEM.getKey(ingredient.item).toString());
      if (ingredient.instrument != null) {
        json.addProperty("instrument", ingredient.instrument.identifier().toString());
      }
      if (ingredient.ignore != null) {
        json.addProperty("ignore", ingredient.ignore.location().toString());
      }
      return json;
    }
  };
  public static final MapCodec<InstrumentIngredient> CODEC = MapCodec.assumeMapUnsafe(CODEC_BODY);
  public static final StreamCodec<RegistryFriendlyByteBuf, InstrumentIngredient> STREAM_CODEC = StreamCodec.of(
    (buffer, ingredient) -> {
      buffer.writeIdentifier(BuiltInRegistries.ITEM.getKey(ingredient.item));
      if (ingredient.instrument != null) {
        buffer.writeByte(1);
        buffer.writeIdentifier(ingredient.instrument.identifier());
      } else if (ingredient.ignore != null) {
        buffer.writeByte(2);
        buffer.writeIdentifier(ingredient.ignore.location());
      } else {
        buffer.writeByte(0);
      }
    },
    buffer -> {
      Item item = BuiltInRegistries.ITEM.getValue(buffer.readIdentifier());
      ResourceKey<Instrument> instrument = null;
      TagKey<Instrument> ignore = null;
      byte type = buffer.readByte();
      if (type == 1) {
        instrument = ResourceKey.create(Registries.INSTRUMENT, buffer.readIdentifier());
      } else if (type == 2) {
        ignore = TagKey.create(Registries.INSTRUMENT, buffer.readIdentifier());
      }
      return new InstrumentIngredient(item, instrument, ignore);
    });

  /** Ingredient type instance */
  public static final IngredientType<InstrumentIngredient> TYPE = new IngredientType<>(CODEC, STREAM_CODEC);
}
