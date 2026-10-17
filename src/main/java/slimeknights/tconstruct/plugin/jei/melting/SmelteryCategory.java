package slimeknights.tconstruct.plugin.jei.melting;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;

/** Smeltery melting, producing only the main output and requiring liquid fuel. */
public class SmelteryCategory extends MeltingCategory {
  private static final Component TITLE = TConstruct.makeTranslation("jei", "smeltery.title");

  public SmelteryCategory(IGuiHelper helper) {
    super(helper, TinkerSmeltery.smelteryController, false);
  }

  @Override
  public RecipeType<MeltingRecipe> getRecipeType() {
    return TConstructJEIConstants.SMELTERY;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  @Override
  protected List<List<FluidStack>> getOutputs(MeltingRecipe recipe) {
    return recipe.getOutputWithByproducts(Config.COMMON.smelteryOreRate).subList(0, 1);
  }
}
