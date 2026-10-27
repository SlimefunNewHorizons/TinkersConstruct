package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.Mantle;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Condition checking for a combination of tags having any entries
 * @param match  List of tags that the entry must match
 * @param ignore Entries in this tag will be ignored towards the match. If null, all entries are considered
 * @param <T>  Registry type
 */
@SuppressWarnings("unused")
public record TagCombinationCondition<T>(List<TagKey<T>> match, @Nullable TagKey<T> ignore) implements ICondition {
  public static final Identifier ID = Mantle.getResource("tag_combination_filled");

  public TagCombinationCondition {
    if (match.isEmpty()) {
      throw new IllegalArgumentException("Must match at least 1 tag");
    }
  }

  /** Creates a new instance ignoring the first tag and matching the rest */
  @SafeVarargs
  public static <T> TagCombinationCondition<T> match(@Nullable TagKey<T> ignore, TagKey<T>... match) {
    return new TagCombinationCondition<>(List.of(match), ignore);
  }

  /** Creates a new instance matching all the passed tags */
  @SafeVarargs
  public static <T> TagCombinationCondition<T> intersection(TagKey<T>... match) {
    return match(null, match);
  }

  /** Creates a new instance matching all the passed tags */
  public static <T> TagCombinationCondition<T> difference(TagKey<T> match, TagKey<T> ignore) {
    return match(ignore, match);
  }


  @Override
  public boolean test(IContext context) {
    // shortcut: the condition context only knows if a tag exists, so require every matched tag to exist and ignore the exclusion tag
    return match.stream().allMatch(context::isTagLoaded);
  }

  private static final Codec<List<Identifier>> MATCH_CODEC = Codec.withAlternative(
    Identifier.CODEC.listOf(), Identifier.CODEC.xmap(List::of, values -> values.get(0)));
  public static final MapCodec<TagCombinationCondition<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    Identifier.CODEC.optionalFieldOf("registry", Registries.ITEM.identifier()).forGetter(value -> value.match.get(0).registry().identifier()),
    MATCH_CODEC.fieldOf("match").forGetter(value -> value.match.stream().map(TagKey::location).toList()),
    Identifier.CODEC.optionalFieldOf("ignore").forGetter(value -> Optional.ofNullable(value.ignore).map(TagKey::location))
  ).apply(instance, TagCombinationCondition::fromCodec));

  private static TagCombinationCondition<?> fromCodec(Identifier registryId, List<Identifier> matches, Optional<Identifier> ignore) {
    ResourceKey<Registry<Object>> registry = ResourceKey.createRegistryKey(registryId);
    return new TagCombinationCondition<>(matches.stream().map(id -> TagKey.create(registry, id)).toList(), ignore.map(id -> TagKey.create(registry, id)).orElse(null));
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }
}
