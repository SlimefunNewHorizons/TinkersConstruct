package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import slimeknights.mantle.util.TagHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Makes the registry ops of the current codec call available to loadables. Since 1.21.2 tags bind after datapack contents load,
 * so ingredients referencing tags must parse with the reload's ops instead of the static registries.
 */
public final class RegistryOpsScope {
  private RegistryOpsScope() {}

  /** Static registries, used outside a codec call. Resolves tags still loading so reload listeners can parse ingredients using new tags */
  private static final RegistryOps<JsonElement> DEFAULT = RegistryOps.create(JsonOps.INSTANCE, HolderLookup.Provider.create(BuiltInRegistries.REGISTRY.stream().map(RegistryOpsScope::pendingLookup)));
  private static final ThreadLocal<RegistryOps<JsonElement>> CURRENT = new ThreadLocal<>();

  /** Runs the action with the given ops in scope, if they are JSON registry ops */
  @SuppressWarnings("unchecked")
  public static <T> T with(DynamicOps<?> ops, Supplier<T> action) {
    if (!(ops instanceof RegistryOps<?> registryOps) || !(ops.empty() instanceof JsonElement)) {
      return action.get();
    }
    RegistryOps<JsonElement> previous = CURRENT.get();
    CURRENT.set((RegistryOps<JsonElement>) registryOps);
    try {
      return action.get();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    }
  }

  /** Creates a lookup that also finds tags which are loading but not yet bound */
  private static <T> HolderLookup.RegistryLookup<T> pendingLookup(Registry<T> registry) {
    return new HolderLookup.RegistryLookup.Delegate<>() {
      @Override
      public HolderLookup.RegistryLookup<T> parent() {
        return registry;
      }

      @Override
      public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
        // the pending set is the one the registry binds once the tags apply, the current set may be replaced
        Optional<HolderSet.Named<T>> pending = TagHelper.getPendingTag(tag);
        return pending.isPresent() ? pending : registry.get(tag);
      }
    };
  }

  /** Gets the ops in scope, or the static registries */
  public static RegistryOps<JsonElement> get() {
    RegistryOps<JsonElement> ops = CURRENT.get();
    return ops != null ? ops : DEFAULT;
  }
}
