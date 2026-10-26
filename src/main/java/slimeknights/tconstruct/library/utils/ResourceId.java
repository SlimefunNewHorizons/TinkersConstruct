package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Helper for use with our extensions of resource location for some type safety in IDs.
 * Note we left {@link Identifier#withPath(String)} and alike as returning {@link Identifier} as there is not much use extending an ID.
 * @see IdParser
 */
public abstract class ResourceId extends Identifier {
  /** Compatibility token retained for the typed ID subclasses. */
  protected static final class Dummy {}

  protected ResourceId(String namespace, String path, @Nullable Dummy pDummy) {
    super(namespace, path);
  }

  public ResourceId(Identifier location) {
    this(location.getNamespace(), location.getPath(), null);
  }

  public ResourceId(String namespace, String path) {
    super(namespace, path);
  }

  public ResourceId(String location) {
    this(Identifier.parse(location));
  }


  /* Helpers for static constructors */

  /**
   * Creates a new ID from the given string
   * @param string  String
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends Identifier> T tryParse(String string, BiFunction<String,String,T> constructor) {
    Identifier location = Identifier.tryParse(string);
    return location == null ? null : constructor.apply(location.getNamespace(), location.getPath());
  }

  /**
   * Creates a new ID from the given namespace and path
   * @param namespace  Namespace
   * @param path       Path
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends Identifier> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (isValidNamespace(namespace) && isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }
}
