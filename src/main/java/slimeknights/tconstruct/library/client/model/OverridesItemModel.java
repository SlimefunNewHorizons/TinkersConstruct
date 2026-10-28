package slimeknights.tconstruct.library.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.client.model.TinkerItemProperties.ItemPropertyFunction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Item model choosing a model like the item model overrides removed in 1.21.4: the last override with all predicates at or above their value wins.
 * Predicates use {@link TinkerItemProperties#PROPERTIES}.
 */
public record OverridesItemModel(ItemModel fallback, List<Override> overrides) implements ItemModel {
  /** Single override */
  public record Override(List<Entry<ItemPropertyFunction,Float>> predicates, ItemModel model) {
    /** Checks if all predicates match */
    boolean matches(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
      for (Entry<ItemPropertyFunction,Float> predicate : predicates) {
        if (predicate.getKey().call(stack, level, entity, seed) < predicate.getValue()) {
          return false;
        }
      }
      return true;
    }
  }

  @java.lang.Override
  public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    LivingEntity entity = owner == null ? null : owner.asLivingEntity();
    for (int i = overrides.size() - 1; i >= 0; i--) {
      Override override = overrides.get(i);
      if (override.matches(stack, level, entity, seed)) {
        override.model.update(state, stack, resolver, context, level, owner, seed);
        return;
      }
    }
    fallback.update(state, stack, resolver, context, level, owner, seed);
  }

  /** Unbaked override */
  public record UnbakedOverride(Map<Identifier,Float> predicate, ItemModel.Unbaked model) {
    private static final Codec<Identifier> PROPERTY = Identifier.CODEC.validate(id -> TinkerItemProperties.PROPERTIES.containsKey(id)
      ? com.mojang.serialization.DataResult.success(id) : com.mojang.serialization.DataResult.error(() -> "Unknown item property " + id));
    public static final Codec<UnbakedOverride> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.unboundedMap(PROPERTY, Codec.FLOAT).fieldOf("predicate").forGetter(UnbakedOverride::predicate),
      ItemModels.CODEC.fieldOf("model").forGetter(UnbakedOverride::model)
    ).apply(inst, UnbakedOverride::new));
  }

  /** Unbaked model */
  public record Unbaked(ItemModel.Unbaked fallback, List<UnbakedOverride> overrides) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      ItemModels.CODEC.fieldOf("fallback").forGetter(Unbaked::fallback),
      UnbakedOverride.CODEC.listOf().fieldOf("overrides").forGetter(Unbaked::overrides)
    ).apply(inst, Unbaked::new));

    @java.lang.Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }

    @java.lang.Override
    public void resolveDependencies(Resolver resolver) {
      fallback.resolveDependencies(resolver);
      for (UnbakedOverride override : overrides) {
        override.model.resolveDependencies(resolver);
      }
    }

    @java.lang.Override
    public ItemModel bake(ItemModel.BakingContext context) {
      return new OverridesItemModel(fallback.bake(context), overrides.stream().map(override -> new Override(
        override.predicate.entrySet().stream().map(entry -> Map.entry(TinkerItemProperties.PROPERTIES.get(entry.getKey()), entry.getValue())).toList(),
        override.model.bake(context))).toList());
    }
  }
}
