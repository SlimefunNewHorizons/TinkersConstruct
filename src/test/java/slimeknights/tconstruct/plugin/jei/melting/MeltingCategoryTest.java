package slimeknights.tconstruct.plugin.jei.melting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.OreMeltingRecipe;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.test.BaseMcTest;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/** Captures the actual JEI output slots without starting a Minecraft client. */
class MeltingCategoryTest extends BaseMcTest {
  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, Map<String,Function<Object[],Object>> handlers) {
    return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (self, method, args) -> {
      if (handlers.containsKey(method.getName())) return handlers.get(method.getName()).apply(args);
      Class<?> result = method.getReturnType();
      if (result == void.class) return null;
      if (result == boolean.class) return false;
      if (result == int.class) return 0;
      if (result.isInterface()) return proxy(result, Map.of());
      return null;
    });
  }

  private static IRecipeLayoutBuilder builder(List<FluidStack> outputs, List<IRecipeSlotTooltipCallback> tooltips) {
    return proxy(IRecipeLayoutBuilder.class, Map.of("addSlot", args ->
      proxySlot(args[0] == RecipeIngredientRole.OUTPUT, outputs, tooltips)));
  }

  @SuppressWarnings("unchecked")
  private static IRecipeSlotBuilder proxySlot(boolean output, List<FluidStack> outputs, List<IRecipeSlotTooltipCallback> tooltips) {
    return proxy(IRecipeSlotBuilder.class, Map.of(
      "setFluidRenderer", a -> proxySlot(output, outputs, tooltips),
      "addTooltipCallback", a -> {
        if (output) tooltips.add((IRecipeSlotTooltipCallback)a[0]);
        return proxySlot(output, outputs, tooltips);
      },
      "addIngredients", a -> {
        if (output) outputs.addAll((List<FluidStack>)a[1]);
        return proxySlot(output, outputs, tooltips);
      }
    ));
  }

  private static OreMeltingRecipe ore() {
    return OreMeltingRecipe.LOADER.deserialize(JsonParser.parseString("""
      {"ingredient":{"item":"minecraft:iron_ingot"},"result":{"fluid":"minecraft:water","amount":90},
       "temperature":950,"time":97,"rate":"metal",
       "byproducts":[{"fluid":"minecraft:lava","amount":25,"rate":"gem"}]}
      """).getAsJsonObject(), slimeknights.mantle.util.typed.TypedMapBuilder.builder()
      .put(slimeknights.mantle.data.loadable.field.ContextKey.ID, TConstruct.getResource("test_ore")).build());
  }

  @Test
  void onlyFoundryShowsByproductsAndEachStructureUsesItsOwnOreRate() {
    MeltingRecipe recipe = ore();
    IGuiHelper gui = proxy(IGuiHelper.class, Map.of());
    List<FluidStack> smelteryOutputs = new ArrayList<>();
    List<IRecipeSlotTooltipCallback> tooltips = new ArrayList<>();
    new SmelteryCategory(gui).setRecipe(builder(smelteryOutputs, tooltips), recipe, proxy(IFocusGroup.class, Map.of()));
    assertThat(smelteryOutputs).hasSize(1);
    assertThat(smelteryOutputs.getFirst().getAmount()).isEqualTo(Config.COMMON.smelteryOreRate.applyOreBoost(OreRateType.METAL, 90));
    assertThat(tooltips).hasSize(1);
    assertThat(tooltips).containsOnly(AbstractMeltingCategory.MeltingFluidCallback.INSTANCE);

    List<FluidStack> melterOutputs = new ArrayList<>();
    new MeltingCategory(gui).setRecipe(builder(melterOutputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
    assertThat(melterOutputs).hasSize(1);
    assertThat(melterOutputs.getFirst().getFluid()).isSameAs(Fluids.WATER);
    assertThat(melterOutputs.getFirst().getAmount()).isEqualTo(Config.COMMON.melterOreRate.applyOreBoost(OreRateType.METAL, 90));
    assertThat(new MeltingCategory(gui).getRecipeType()).isSameAs(TConstructJEIConstants.MELTING);
    assertThat(new SmelteryCategory(gui).getRecipeType()).isSameAs(TConstructJEIConstants.SMELTERY);

    List<FluidStack> foundryOutputs = new ArrayList<>();
    new FoundryCategory(gui).setRecipe(builder(foundryOutputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
    assertThat(foundryOutputs).hasSize(2);
    assertThat(foundryOutputs.getFirst().getAmount()).isEqualTo(Config.COMMON.foundryOreRate.applyOreBoost(OreRateType.METAL, 90));
    assertThat(foundryOutputs.getLast().getAmount()).isEqualTo(Config.COMMON.foundryByproductRate.applyOreBoost(OreRateType.GEM, 25));
    assertThat(new FoundryCategory(gui).getRecipeType()).isSameAs(TConstructJEIConstants.FOUNDRY);
  }

  @Test
  void plainRecipeByproductsAreOnlyShownInTheFoundryLayout() {
    MeltingRecipe recipe = new MeltingRecipe(TConstruct.getResource("test_multiple"), "", Ingredient.of(Items.IRON_INGOT),
      FluidOutput.fromFluid(Fluids.WATER, 90), 800, 1,
      List.of(FluidOutput.fromFluid(Fluids.LAVA, 30), FluidOutput.fromFluid(Fluids.WATER, 20)), false);
    List<FluidStack> outputs = new ArrayList<>();
    new SmelteryCategory(proxy(IGuiHelper.class, Map.of())).setRecipe(builder(outputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
    assertThat(outputs).extracting(FluidStack::getAmount).containsExactly(90);
    List<FluidStack> melterOutputs = new ArrayList<>();
    new MeltingCategory(proxy(IGuiHelper.class, Map.of())).setRecipe(builder(melterOutputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
    assertThat(melterOutputs).extracting(FluidStack::getAmount).containsExactly(90);
    List<FluidStack> foundryOutputs = new ArrayList<>();
    new FoundryCategory(proxy(IGuiHelper.class, Map.of())).setRecipe(builder(foundryOutputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
    assertThat(foundryOutputs).extracting(FluidStack::getAmount).containsExactly(90, 30, 20);
  }

  private static Path ironRecipeDirectory() {
    Path base = Path.of("").toAbsolutePath();
    for (int i = 0; i < 4 && base != null; i++, base = base.getParent()) {
      Path directory = base.resolve("src/generated/resources/data/tconstruct/recipe/smeltery/melting/metal/iron");
      if (Files.isDirectory(directory)) return directory;
    }
    throw new IllegalStateException("Could not find generated iron recipes from " + Path.of("").toAbsolutePath());
  }

  @Test
  void shippedIronOreRecipesKeepSteelByproductsInTheFoundryCategory() throws IOException {
    for (String name : List.of("raw", "raw_block", "ore_sparse", "ore_singular", "ore_dense")) {
      JsonObject json = JsonParser.parseString(Files.readString(ironRecipeDirectory().resolve(name + ".json"))).getAsJsonObject();
      JsonObject result = json.getAsJsonObject("result");
      JsonObject byproduct = json.getAsJsonArray("byproducts").get(0).getAsJsonObject();
      assertThat(result.get("tag").getAsString()).as("%s main output", name).isEqualTo("c:molten_iron");
      assertThat(byproduct.get("tag").getAsString()).as("%s byproduct", name).isEqualTo("c:molten_steel");
      int baseAmount = result.get("amount").getAsInt();
      int baseByproduct = byproduct.get("amount").getAsInt();
      // Resolve the shipped fluid tags directly; the unit test launcher does not bind data pack tags.
      result.remove("tag");
      result.addProperty("fluid", "tconstruct:molten_iron");
      byproduct.remove("tag");
      byproduct.addProperty("fluid", "tconstruct:molten_steel");
      json.add("ingredient", JsonParser.parseString("{\"item\":\"minecraft:raw_iron\"}"));
      OreMeltingRecipe recipe = OreMeltingRecipe.LOADER.deserialize(json, slimeknights.mantle.util.typed.TypedMapBuilder.builder()
        .put(slimeknights.mantle.data.loadable.field.ContextKey.ID, TConstruct.getResource("test_iron/" + name)).build());
      IGuiHelper gui = proxy(IGuiHelper.class, Map.of());
      for (MeltingCategory category : List.of(new MeltingCategory(gui), new SmelteryCategory(gui))) {
        List<FluidStack> outputs = new ArrayList<>();
        category.setRecipe(builder(outputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
        assertThat(outputs).as("%s in %s", name, category.getRecipeType()).hasSize(1);
        assertThat(outputs.getFirst().getFluid()).isSameAs(TinkerFluids.moltenIron.get());
        var rate = category instanceof SmelteryCategory ? Config.COMMON.smelteryOreRate : Config.COMMON.melterOreRate;
        assertThat(outputs.getFirst().getAmount()).isEqualTo(rate.applyOreBoost(OreRateType.METAL, baseAmount));
      }
      List<FluidStack> foundryOutputs = new ArrayList<>();
      new FoundryCategory(gui).setRecipe(builder(foundryOutputs, new ArrayList<>()), recipe, proxy(IFocusGroup.class, Map.of()));
      assertThat(foundryOutputs).as("%s in foundry", name).hasSize(2);
      assertThat(foundryOutputs.getFirst().getFluid()).isSameAs(TinkerFluids.moltenIron.get());
      assertThat(foundryOutputs.getFirst().getAmount()).isEqualTo(Config.COMMON.foundryOreRate.applyOreBoost(OreRateType.METAL, baseAmount));
      assertThat(foundryOutputs.getLast().getFluid()).isSameAs(TinkerFluids.moltenSteel.get());
      assertThat(foundryOutputs.getLast().getAmount()).isEqualTo(Config.COMMON.foundryByproductRate.applyOreBoost(OreRateType.METAL, baseByproduct));
    }
  }

  @Test
  void displayRecomputesRatesAndDoesNotExposeMutableRecipeStacks() {
    MeltingRecipe recipe = ore();
    AtomicInteger multiplier = new AtomicInteger(2);
    var rate = (slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate)(type, amount) -> amount * multiplier.get();
    List<List<FluidStack>> first = recipe.getOutputWithByproducts(rate);
    assertThat(first.getFirst().getFirst().getAmount()).isEqualTo(180);
    first.getLast().getFirst().setAmount(1);
    multiplier.set(3);
    List<List<FluidStack>> second = recipe.getOutputWithByproducts(rate);
    assertThat(second.getFirst().getFirst().getAmount()).isEqualTo(270);
    assertThat(second.getLast().getFirst().getAmount()).isEqualTo(Config.COMMON.foundryByproductRate.applyOreBoost(OreRateType.GEM, 25));
  }
}
