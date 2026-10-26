package slimeknights.mantle.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Tracks the registry key of the object currently being constructed by a Mantle register.
 * Since 1.21.2 item and block properties must hold their registry key before construction; Mantle mixins fill it in from here,
 * so registration code written before that change keeps working, including properties shared between several objects.
 */
public final class RegistrationContext {
  private RegistrationContext() {}

  private static final ThreadLocal<Deque<ResourceKey<?>>> KEYS = ThreadLocal.withInitial(ArrayDeque::new);
  /** Set by the block item mixin while a {@link net.minecraft.world.item.BlockItem} constructor runs */
  private static final ThreadLocal<Boolean> BLOCK_ITEM = ThreadLocal.withInitial(() -> false);

  /** Runs the supplier with the given key as the current registry key */
  public static <T> T with(ResourceKey<?> key, Supplier<T> supplier) {
    Deque<ResourceKey<?>> keys = KEYS.get();
    keys.push(key);
    try {
      return supplier.get();
    } finally {
      keys.pop();
    }
  }

  /** Gets the key of the object being constructed if it belongs to the given registry */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> ResourceKey<T> current(ResourceKey<? extends Registry<T>> registry) {
    ResourceKey<?> key = KEYS.get().peek();
    return key != null && key.isFor(registry) ? (ResourceKey<T>) key : null;
  }

  /** Marks that a block item is being constructed, consumed by the next description ID lookup */
  public static void markBlockItem() {
    BLOCK_ITEM.set(true);
  }

  /** Checks and clears the block item mark */
  public static boolean consumeBlockItem() {
    boolean blockItem = BLOCK_ITEM.get();
    BLOCK_ITEM.set(false);
    return blockItem;
  }
}
