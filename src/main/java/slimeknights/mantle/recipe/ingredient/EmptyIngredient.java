package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.Mantle;
import net.minecraft.resources.Identifier;

import java.util.stream.Stream;

/**
 * Ingredient matching only empty stacks. Replaces {@code Ingredient.EMPTY}, as vanilla ingredients cannot be empty since 1.21.2.
 */
public enum EmptyIngredient implements ICustomIngredient {
  INSTANCE;

  public static final Identifier ID = Mantle.getResource("empty");
  public static final IngredientType<EmptyIngredient> TYPE = new IngredientType<>(MapCodec.unit(INSTANCE), StreamCodec.<RegistryFriendlyByteBuf, EmptyIngredient>unit(INSTANCE));

  @Override
  public boolean test(ItemStack stack) {
    return stack.isEmpty();
  }

  @Override
  public Stream<Holder<Item>> items() {
    return Stream.empty();
  }

  @Override
  public boolean isSimple() {
    // simple ingredients sync as a holder set, which may not be empty
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public SlotDisplay display() {
    return SlotDisplay.Empty.INSTANCE;
  }
}
