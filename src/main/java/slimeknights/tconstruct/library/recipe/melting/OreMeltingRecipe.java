package slimeknights.tconstruct.library.recipe.melting;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.field.MergingListField;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Extension of melting recipe to boost results of ores
 */
public class OreMeltingRecipe extends MeltingRecipe {
  public static final RecordLoadable<OreMeltingRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP, INPUT, OUTPUT, TEMPERATURE, TIME, BYPRODUCTS,
    TinkerLoadables.ORE_RATE_TYPE.requiredField("rate", OreMeltingRecipe::getOreType),
    new MergingListField<>(TinkerLoadables.ORE_RATE_TYPE.defaultField("rate", OreRateType.DEFAULT, Function.identity()), "byproducts", r -> r.byproductTypes),
    OreMeltingRecipe::new);

  @Getter
  private final OreRateType oreType;
  private final List<OreRateType> byproductTypes;
  protected OreMeltingRecipe(ResourceLocation id, String group, Ingredient input, FluidOutput output, int temperature, int time, List<FluidOutput> byproducts, OreRateType oreType, List<OreRateType> byproductTypes) {
    super(id, group, input, output, temperature, time, byproducts);
    this.oreType = oreType;
    this.byproductTypes = byproductTypes;
  }

  @Override
  public FluidStack getOutput(IMeltingContainer inv) {
    return inv.getOreRate().applyOreBoost(oreType, output.get(), true);
  }

  @Override
  public List<FluidStack> getByproducts(IMeltingContainer inv) {
    List<FluidStack> outputs = new ArrayList<>(byproducts.size());
    for (int i = 0; i < byproducts.size(); i++) {
      OreRateType rate = i < byproductTypes.size() ? byproductTypes.get(i).orElse(oreType) : oreType;
      outputs.add(Config.COMMON.foundryByproductRate.applyOreBoost(rate, byproducts.get(i).get(), true));
    }
    return outputs;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerSmeltery.oreMeltingSerializer.get();
  }
}
