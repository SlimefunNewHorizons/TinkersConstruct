package slimeknights.tconstruct.plugin.jei.melting;

import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.AlloyRecipeCategory;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;
import java.util.function.Function;

/** Melter recipes, also providing the shared layout for the smeltery category. */
public class MeltingCategory extends AbstractMeltingCategory {
  private static final Component TITLE = TConstruct.makeTranslation("jei", "melter.title");

  /** Tooltip callback for items */
  private static final IRecipeSlotTooltipCallback ITEM_FUEL_TOOLTIP = (slot, list) -> {
    MeltingFuel solid = MeltingFuelLookup.getSolid();
    list.add(1, Component.translatable(KEY_TEMPERATURE, solid.getTemperature()).withStyle(ChatFormatting.GRAY));
    list.add(2, Component.translatable(KEY_MULTIPLIER, solid.getRate() / 10f).withStyle(ChatFormatting.GRAY));
  };

  @Getter
  private final IDrawable icon;
  private final IDrawableStatic solidFuel;
  private final boolean showSolidFuel;

  public MeltingCategory(IGuiHelper helper) {
    this(helper, TinkerSmeltery.searedMelter, true);
  }

  protected MeltingCategory(IGuiHelper helper, ItemLike icon, boolean showSolidFuel) {
    super(helper);
    this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(icon));
    this.solidFuel = helper.drawableBuilder(BACKGROUND_LOC, 164, 0, 18, 20).build();
    this.showSolidFuel = showSolidFuel;
  }

  @Override
  public RecipeType<MeltingRecipe> getRecipeType() {
    return TConstructJEIConstants.MELTING;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  @Override
  public void draw(MeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
    super.draw(recipe, slots, graphics, mouseX, mouseY);

    // solid fuel slot
    int temperature = recipe.getTemperature();
    if (showSolidFuel && temperature <= MeltingFuelLookup.getSolid().getTemperature()) {
      solidFuel.draw(graphics, 1, 19);
    }
  }

  /** The melter only produces the main output, even when the recipe defines byproducts. */
  protected List<List<FluidStack>> getOutputs(MeltingRecipe recipe) {
    return recipe.getOutputWithByproducts(Config.COMMON.melterOreRate).subList(0, 1);
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
    // input
    builder.addSlot(RecipeIngredientRole.INPUT, 24, 18).addIngredients(recipe.getInput());

    // output
    List<List<FluidStack>> outputs = getOutputs(recipe);
    AlloyRecipeCategory.drawVariableFluids(builder, RecipeIngredientRole.OUTPUT, 96, 4, 32, 32, outputs,
      FluidValues.METAL_BLOCK, Function.identity(), list -> MeltingFluidCallback.INSTANCE);

    // show fuels that are valid for this recipe
    int fuelHeight = 32;
    // solid fuel
    if (showSolidFuel && recipe.getTemperature() <= MeltingFuelLookup.getSolid().getTemperature()) {
      fuelHeight = 15;
      builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 2, 22)
             .addTooltipCallback(ITEM_FUEL_TOOLTIP)
             .addItemStacks(MeltingFuelHandler.SOLID_FUELS.get());
    }

    // liquid fuel
    builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 4, 4)
           .addTooltipCallback(FUEL_TOOLTIP)
           .setFluidRenderer(1, false, 12, fuelHeight)
           .addIngredients(NeoForgeTypes.FLUID_STACK, MeltingFuelHandler.getUsableFuels(recipe.getTemperature()));
  }
}
