package slimeknights.tconstruct.library.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Harvest level display names and ordering. Since 1.21.2 a harvest tier is a {@link ToolMaterial}.
 */
public class HarvestTiers {
  private HarvestTiers() {}

  /** Vanilla tiers by ID, in ascending order */
  private static final Map<Identifier, ToolMaterial> VANILLA = ImmutableMap.<Identifier, ToolMaterial>builder()
    .put(Identifier.withDefaultNamespace("wood"), ToolMaterial.WOOD)
    .put(Identifier.withDefaultNamespace("gold"), ToolMaterial.GOLD)
    .put(Identifier.withDefaultNamespace("stone"), ToolMaterial.STONE)
    .put(Identifier.withDefaultNamespace("copper"), ToolMaterial.COPPER)
    .put(Identifier.withDefaultNamespace("iron"), ToolMaterial.IRON)
    .put(Identifier.withDefaultNamespace("diamond"), ToolMaterial.DIAMOND)
    .put(Identifier.withDefaultNamespace("netherite"), ToolMaterial.NETHERITE)
    .build();
  /** Cache of name for each tier */
  private static final Map<ToolMaterial, Component> harvestLevelNames = Maps.newConcurrentMap();
  /** Registries used to resolve non-vanilla tiers, queried in registration order. Multiple mods may provide tiers, so a resolver must not replace an existing one. */
  private static final List<TierResolver> tierResolvers = Lists.newArrayList();
  /** Placeholders for tiers that are not registered yet, cached so all lookups of one ID share an instance */
  private static final Map<Identifier, ToolMaterial> placeholders = Maps.newConcurrentMap();
  /** Reverse lookup of {@link #placeholders} */
  private static final Map<ToolMaterial, Identifier> placeholderIds = Maps.newConcurrentMap();
  /** Real tiers resolved for placeholders */
  private static final Map<Identifier, ToolMaterial> resolvedPlaceholders = Maps.newConcurrentMap();
  /** Listener to clear name cache so we get new colors, and to let tiers from data driven sources resolve once again */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> {
    harvestLevelNames.clear();
    resolvedPlaceholders.clear();
  };

  /** Adds a registry used to resolve non-vanilla tiers. Resolvers are queried in registration order, so registering a tier never hides tiers provided by other mods. */
  public static void registerTierResolver(Function<Identifier, ToolMaterial> lookup, Function<ToolMaterial, Identifier> idLookup) {
    tierResolvers.add(new TierResolver(lookup, idLookup));
  }

  /** Makes a translation key for the given name */
  private static MutableComponent makeLevelKey(ToolMaterial tier) {
    Identifier id = getId(tier);
    String key = Util.makeTranslationKey("harvest_tier", id == null ? TConstruct.getResource("unknown") : id);
    TextColor color = ResourceColorManager.getTextColor(key);
    return TConstruct.makeTranslation("stat", key).withStyle(style -> style.withColor(color));
  }

  /**
   * Gets the harvest level name for the given level number
   * @param tier  Tier
   * @return  Level name
   */
  public static Component getName(ToolMaterial tier) {
    return harvestLevelNames.computeIfAbsent(tier, HarvestTiers::makeLevelKey);
  }

  /** Gets the larger of two tiers */
  public static ToolMaterial max(ToolMaterial a, ToolMaterial b) {
    return compare(b, a) > 0 ? b : a;
  }

  /** Gets the smaller of two tiers */
  public static ToolMaterial min(ToolMaterial a, ToolMaterial b) {
    return compare(b, a) < 0 ? b : a;
  }

  /** Gets the smallest tier in the sorting registry */
  public static ToolMaterial minTier() {
    return ToolMaterial.WOOD;
  }

  /** Gets the tag of blocks the tier cannot harvest, resolving placeholders */
  public static TagKey<Block> incorrectBlocksForDrops(ToolMaterial tier) {
    return unwrap(tier).incorrectBlocksForDrops();
  }

  /** Gets the stable datapack ID for a tier, which works for tiers that are not registered yet */
  @Nullable
  public static Identifier getId(ToolMaterial tier) {
    Identifier placeholder = placeholderIds.get(tier);
    if (placeholder != null) {
      return placeholder;
    }
    for (Map.Entry<Identifier, ToolMaterial> entry : VANILLA.entrySet()) {
      if (entry.getValue() == tier) {
        return entry.getKey();
      }
    }
    for (TierResolver resolver : tierResolvers) {
      Identifier id = resolver.idLookup().apply(tier);
      if (id != null) {
        return id;
      }
    }
    // records compare by value, so a copy of a vanilla tier is still that tier
    for (Map.Entry<Identifier, ToolMaterial> entry : VANILLA.entrySet()) {
      if (entry.getValue().equals(tier)) {
        return entry.getKey();
      }
    }
    return null;
  }

  /** Resolves a tier ID, returning null when the tier is not registered. Data driven tiers may legitimately be missing while their data loads. */
  @Nullable
  public static ToolMaterial byId(Identifier id) {
    ToolMaterial vanilla = VANILLA.get(id);
    if (vanilla != null) {
      return vanilla;
    }
    for (TierResolver resolver : tierResolvers) {
      ToolMaterial tier = resolver.lookup().apply(id);
      if (tier != null) {
        return tier;
      }
    }
    return null;
  }

  /**
   * Resolves a tier ID, falling back to a placeholder when the tier is not registered yet.
   * Tiers from data packs or from Json Things may load after the content referencing them, so an unknown tier must not fail the load.
   */
  public static ToolMaterial byIdOrPlaceholder(Identifier id) {
    ToolMaterial tier = byId(id);
    if (tier != null) {
      return tier;
    }
    return placeholders.computeIfAbsent(id, missing -> {
      TConstruct.LOG.warn("Unknown harvest tier {}, treating it as the lowest tier until it resolves. This is expected while data driven tiers are still loading; "
        + "if it persists, check that the mod or datapack providing this tier loaded successfully. Registered tier resolvers: {}", missing, tierResolvers.size());
      // the repair tag makes the record unique per ID, so placeholders never compare equal to each other or a real tier
      ToolMaterial placeholder = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 0, 0, 0, 0, TagKey.create(Registries.ITEM, missing));
      placeholderIds.put(placeholder, missing);
      return placeholder;
    });
  }

  /** Gets the real tier behind a placeholder, or the tier itself. Unresolved placeholders act as the lowest tier */
  private static ToolMaterial unwrap(ToolMaterial tier) {
    ToolMaterial resolved = resolveOrNull(tier);
    return resolved != null ? resolved : minTier();
  }

  /** Gets the real tier behind a placeholder, or null if it is not available yet */
  @Nullable
  private static ToolMaterial resolveOrNull(ToolMaterial tier) {
    Identifier id = placeholderIds.get(tier);
    if (id == null) {
      return tier;
    }
    ToolMaterial resolved = resolvedPlaceholders.get(id);
    if (resolved == null) {
      resolved = byId(id);
      if (resolved != null) {
        resolvedPlaceholders.put(id, resolved);
      }
    }
    return resolved;
  }

  private static int compare(ToolMaterial a, ToolMaterial b) {
    if (a == b) {
      return 0;
    }
    // a tier waiting on its data may end up larger than any registered tier, so keep it on top rather than dropping its ID
    if (resolveOrNull(a) == null) {
      return 1;
    }
    if (resolveOrNull(b) == null) {
      return -1;
    }
    Set<Block> aIncorrect = incorrectBlocks(a);
    Set<Block> bIncorrect = incorrectBlocks(b);
    if (!aIncorrect.equals(bIncorrect)) {
      if (aIncorrect.containsAll(bIncorrect)) {
        return -1;
      }
      if (bIncorrect.containsAll(aIncorrect)) {
        return 1;
      }
    }
    return Integer.compare(vanillaRank(a), vanillaRank(b));
  }

  private static Set<Block> incorrectBlocks(ToolMaterial tier) {
    return BuiltInRegistries.BLOCK.get(incorrectBlocksForDrops(tier))
      .map(tag -> tag.stream().map(holder -> holder.value()).collect(Collectors.toSet()))
      .orElseGet(Set::of);
  }

  private static int vanillaRank(ToolMaterial tier) {
    ToolMaterial real = unwrap(tier);
    if (real.equals(ToolMaterial.WOOD) || real.equals(ToolMaterial.GOLD)) return 0;
    if (real.equals(ToolMaterial.STONE)) return 1;
    if (real.equals(ToolMaterial.COPPER)) return 2;
    if (real.equals(ToolMaterial.IRON)) return 3;
    if (real.equals(ToolMaterial.DIAMOND)) return 4;
    if (real.equals(ToolMaterial.NETHERITE)) return 5;
    return -1;
  }

  /** Pair of functions resolving tiers from one registry */
  private record TierResolver(Function<Identifier, ToolMaterial> lookup, Function<ToolMaterial, Identifier> idLookup) {}
}
