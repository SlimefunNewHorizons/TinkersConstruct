package slimeknights.tconstruct.plugin.jsonthings;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ToolMaterial;

import javax.annotation.Nullable;

/** Optional bridge to tiers registered by Json Things. */
final class JsonThingsTierRegistry {
  private JsonThingsTierRegistry() {}

  @Nullable
  static ToolMaterial byId(Identifier id) {
    return ThingRegistries.TOOL_MATERIAL.getValue(id);
  }

  @Nullable
  static Identifier getId(ToolMaterial tier) {
    return ThingRegistries.TOOL_MATERIAL.getKey(tier);
  }
}
