package slimeknights.tconstruct.library.tools.layout;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StationSlotLayoutTest extends BaseMcTest {
  @Test
  void layoutSlot_bufferReadWrite() {
    LayoutSlot slot = new LayoutSlot(new Pattern("test:pattern"), "name", 5, 6, Ingredient.of(Items.BOOK));
    FriendlyByteBuf buffer = registryBuffer();
    slot.write(buffer);

    LayoutSlot decoded = LayoutSlot.read(buffer);
    Pattern pattern = decoded.getIcon();
    assertThat(pattern).isNotNull();
    assertThat(pattern.toString()).isEqualTo("test:pattern");
    assertThat(decoded.getTranslationKey()).isEqualTo("name");
    assertThat(decoded.getX()).isEqualTo(5);
    assertThat(decoded.getY()).isEqualTo(6);
    Ingredient ingredient = decoded.getFilter();
    assertThat(ingredient).isNotNull();
    List<Holder<Item>> items = ingredient.items().toList();
    assertThat(items).hasSize(1);
    assertThat(items.get(0).value()).isEqualTo(Items.BOOK);
  }

  @Test
  void stationLayout_getSlot() {
    StationSlotLayout layout = StationSlotLayout
      .builder()
      .toolSlot(1, 2)
      .addInputSlot(null, 3, 4)
      .addInputSlot(null, 5, 6)
      .build();

    LayoutSlot slot = layout.getSlot(0);
    assertThat(slot.getX()).isEqualTo(1);
    assertThat(slot.getY()).isEqualTo(2);
    slot = layout.getSlot(1);
    assertThat(slot.getX()).isEqualTo(3);
    assertThat(slot.getY()).isEqualTo(4);
    slot = layout.getSlot(2);
    assertThat(slot.getX()).isEqualTo(5);
    assertThat(slot.getY()).isEqualTo(6);
    slot = layout.getSlot(3);
    assertThat(slot.getX()).isEqualTo(-1);
    assertThat(slot.getY()).isEqualTo(-1);
    slot = layout.getSlot(-1);
    assertThat(slot.getX()).isEqualTo(-1);
    assertThat(slot.getY()).isEqualTo(-1);
  }

  // decoded tested in packet test
}
