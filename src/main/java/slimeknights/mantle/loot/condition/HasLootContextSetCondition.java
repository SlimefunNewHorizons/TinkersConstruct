package slimeknights.mantle.loot.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import slimeknights.mantle.loot.MantleLoot;


/** Loot condition that only runs if all required values in the given loot context set are present. Good heuristic for using that set. */
public record HasLootContextSetCondition(ContextKeySet set) implements LootItemCondition {
  public static final MapCodec<HasLootContextSetCondition> CODEC = LootContextParamSets.CODEC
    .xmap(HasLootContextSetCondition::new, HasLootContextSetCondition::set).fieldOf("set");
  /** Creates a new builder instance */
  public static Builder builder(ContextKeySet set) {
    return new Builder(set);
  }

  @Override
  public LootItemConditionType getType() {
    return MantleLoot.HAS_CONTEXT_SET;
  }

  @Override
  public boolean test(LootContext context) {
    for (ContextKey<?> param : set.required()) {
      if (!context.hasParameter(param)) {
        return false;
      }
    }
    return true;
  }

  /** Builder logic for this condition */
  public record Builder(ContextKeySet set) implements LootItemCondition.Builder {
    @Override
    public LootItemCondition build() {
      return new HasLootContextSetCondition(set);
    }
  }
}
