package slimeknights.tconstruct.library.json.condition;

import slimeknights.mantle.util.TagHelper;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.TConstruct;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagCombinationCondition#difference(TagKey, TagKey)} */
@Deprecated(forRemoval = true)
public class TagDifferencePresentCondition<T> implements ICondition {
  private static final Identifier NAME = TConstruct.getResource("tag_difference_present");
  public static final MapCodec<TagDifferencePresentCondition<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    Identifier.CODEC.fieldOf("registry").forGetter(value -> value.base.registry().identifier()),
    Identifier.CODEC.fieldOf("base").forGetter(value -> value.base.location()),
    Identifier.CODEC.listOf().fieldOf("subtracted").forGetter(value -> value.subtracted.stream().map(TagKey::location).toList())
  ).apply(instance, TagDifferencePresentCondition::fromLocations));

  private final TagKey<T> base;
  private final List<TagKey<T>> subtracted;

  public TagDifferencePresentCondition(TagKey<T> base, List<TagKey<T>> subtracted) {
    if (subtracted.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a condition with no subtracted");
    }
    this.base = base;
    this.subtracted = subtracted;
  }

  /** Creates a condition from a set of keys */
  @SafeVarargs
  public static <T> TagDifferencePresentCondition<T> ofKeys(TagKey<T> base, TagKey<T>... subtracted) {
    return new TagDifferencePresentCondition<>(base, Arrays.asList(subtracted));
  }

  /** Creates a condition from a registry and a set of names */
  public static <T> TagDifferencePresentCondition<T> ofNames(ResourceKey<? extends Registry<T>> registry, Identifier base, Identifier... subtracted) {
    TagKey<T> baseKey = TagKey.create(registry, base);
    return new TagDifferencePresentCondition<>(baseKey, Arrays.stream(subtracted).map(name -> TagKey.create(registry, name)).toList());
  }

  private static TagDifferencePresentCondition<?> fromLocations(Identifier registryName, Identifier base, List<Identifier> subtracted) {
    ResourceKey<? extends Registry<Object>> registry = ResourceKey.createRegistryKey(registryName);
    return new TagDifferencePresentCondition<>(TagKey.create(registry, base), subtracted.stream().map(id -> TagKey.create(registry, id)).toList());
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    // get the base tag
    Collection<Holder<T>> base = TagHelper.getTag(context, this.base);
    if (base == null || base.isEmpty()) {
      return false;
    }

    // no subtracted tags? we good
    if (subtracted.isEmpty()) {
      return true;
    }
    // all tags have something, so find the first item that is in all tags
    itemLoop:
    for (Holder<T> entry : base) {
      // find the first item contained in no subtracted tags
      for (TagKey<T> tag : subtracted) {
        if (TagHelper.getTag(context, tag).contains(entry)) {
          continue itemLoop;
        }
      }
      // no subtracted contains the item? success
      return true;
    }
    // no item not in any subtracted
    return false;
  }

}
