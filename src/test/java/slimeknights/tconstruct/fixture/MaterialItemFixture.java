package slimeknights.tconstruct.fixture;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.test.TestHelper;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

public class MaterialItemFixture {

  public static ToolPartItem MATERIAL_ITEM, MATERIAL_ITEM_2, MATERIAL_ITEM_HEAD, MATERIAL_ITEM_HANDLE, MATERIAL_ITEM_EXTRA;

  private MaterialItemFixture() {
  }

  private static boolean init = false;
  @SuppressWarnings("unchecked")  // its correct, and if it were to fail this is tests
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    ((MappedRegistry<Item>)BuiltInRegistries.ITEM).unfreeze(false); // yes, I know this is bad, but this is testing so we do bad things sometimes
    MATERIAL_ITEM = register("test_material", MaterialStatsFixture.STATS_TYPE);
    MATERIAL_ITEM_2 = register("test_material_2", MaterialStatsFixture.STATS_TYPE_2);
    MATERIAL_ITEM_HEAD = register("test_head", HeadMaterialStats.ID);
    MATERIAL_ITEM_HANDLE = register("test_handle", HandleMaterialStats.ID);
    MATERIAL_ITEM_EXTRA = register("test_extra", StatlessMaterialStats.BINDING.getIdentifier());
  }

  private static ToolPartItem register(String name, MaterialStatsId statType) {
    return TestHelper.registerItem(name, properties -> new ToolPartItem(properties, statType));
  }
}
