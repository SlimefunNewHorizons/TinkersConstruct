package slimeknights.tconstruct.smeltery.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.SmelteryAlloyTank;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Audits every shipped furnace output, including optional compat recipes, against its JSON and network round trip.
 * Tests resolve fluid tags from the shipped data without mutating global registry tags. Optional mod inputs use a
 * registered test item so their output math can be checked even when those mods are absent from the test launcher.
 */
class FurnaceRecipeOutputTest extends BaseMcTest {
  private static final Path DATA = dataRoot();
  private static final Set<String> MELTING = Set.of("tconstruct:melting", "tconstruct:ore_melting", "tconstruct:damagable_melting", "tconstruct:material_melting");
  private static final Set<String> OTHER = Set.of("tconstruct:alloy", "tconstruct:entity_melting", "tconstruct:melting_fuel", "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking");

  private static Path dataRoot() {
    Path base = Path.of("").toAbsolutePath();
    for (int i = 0; i < 4 && base != null; i++, base = base.getParent()) {
      Path data = base.resolve("src/generated/resources/data");
      if (Files.isDirectory(data)) return data;
    }
    throw new IllegalStateException("Could not find generated recipe data from " + Path.of("").toAbsolutePath());
  }

  private record Input(ItemStack stack, IOreRate oreRate) implements IMeltingContainer {
    @Override public ItemStack getStack() { return stack; }
    @Override public IOreRate getOreRate() { return oreRate; }
  }

  private static Fluid resolveTag(String name, Set<String> visited) throws IOException {
    if (!visited.add(name)) return Fluids.EMPTY;
    Identifier id = Identifier.parse(name);
    Path tag = DATA.resolve(id.getNamespace() + "/tags/fluid/" + id.getPath() + ".json");
    if (!Files.exists(tag)) return Fluids.EMPTY;
    JsonArray values = JsonParser.parseString(Files.readString(tag)).getAsJsonObject().getAsJsonArray("values");
    for (JsonElement element : values) {
      String value = element.isJsonPrimitive() ? element.getAsString() : element.getAsJsonObject().get("id").getAsString();
      Fluid fluid = value.startsWith("#") ? resolveTag(value.substring(1), visited)
        : BuiltInRegistries.FLUID.get(Identifier.parse(value));
      if (fluid != Fluids.EMPTY) return fluid;
    }
    return Fluids.EMPTY;
  }

  private static void resolveFluids(JsonElement element, JsonObject source) throws IOException {
    if (element == null) return;
    if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) resolveFluids(child, source);
    } else if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("tag")) {
        String tag = object.get("tag").getAsString();
        Fluid fluid = resolveTag(tag, new HashSet<>());
        if (fluid == Fluids.EMPTY) {
          // Dawnstone is provided entirely by an optional mod, and must be guarded by a fluid tag condition.
          assertThat(source.getAsJsonArray("neoforge:conditions")).as("condition guarding external fluid %s", tag).isNotNull();
          assertThat(source.getAsJsonArray("neoforge:conditions").asList()).anySatisfy(condition -> {
            JsonObject json = condition.getAsJsonObject();
            assertThat(json.get("registry")).isNotNull();
            assertThat(json.get("registry").getAsString()).isEqualTo("minecraft:fluid");
            assertThat(json.get("tag").getAsString()).isEqualTo(tag);
          });
          fluid = Fluids.WATER;
        }
        object.remove("tag");
        object.addProperty("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
      }
      for (JsonElement child : object.asMap().values()) {
        if (child.isJsonArray() || child.isJsonObject()) resolveFluids(child, source);
      }
    }
  }

  private static Recipe<?> roundTrip(Identifier id, Recipe<?> recipe) {
    var buffer = registryBuffer();
    try {
      RecipeHolder.STREAM_CODEC.encode(buffer, new RecipeHolder<>(id, recipe));
      return RecipeHolder.STREAM_CODEC.decode(buffer).value();
    } finally {
      buffer.release();
    }
  }

  private static void assertFluid(FluidStack actual, JsonObject expected, int amount) {
    assertThat(actual.getFluid()).isSameAs(BuiltInRegistries.FLUID.get(Identifier.parse(expected.get("fluid").getAsString())));
    assertThat(actual.getAmount()).isEqualTo(amount);
  }

  private static OreRateType rate(JsonObject json, OreRateType fallback) {
    return json.has("rate") ? OreRateType.valueOf(json.get("rate").getAsString().toUpperCase(java.util.Locale.ROOT)).orElse(fallback) : fallback;
  }

  private static int damagedAmount(JsonObject fluid, int defaultUnit, ItemStack stack) {
    int unit = fluid.has("unit_size") ? fluid.get("unit_size").getAsInt() : defaultUnit;
    int amount = (int)((long)fluid.get("amount").getAsInt() * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
    return Math.max(unit, amount - amount % unit);
  }

  /** Exercises the controller's actual inventory and tank, with recipe lookup supplied by a minimal world. */
  private static void assertMachineOutputs(IMeltingRecipe recipe, ItemStack stack, JsonObject result, int amount,
                                           List<FluidStack> byproducts, boolean foundry) {
    HeatingStructureBlockEntity machine = foundry
      ? new FoundryBlockEntity(BlockPos.ZERO, TinkerSmeltery.foundryController.get().defaultBlockState())
      : new SmelteryBlockEntity(BlockPos.ZERO, TinkerSmeltery.smelteryController.get().defaultBlockState());
    machine.getTank().setCapacity(Integer.MAX_VALUE);
    machine.getMeltingInventory().resize(1, ignored -> {});
    var module = machine.getMeltingInventory().getModule(0);
    Level level = mock(Level.class);
    RecipeManager manager = mock(RecipeManager.class);
    when(level.getRecipeManager()).thenReturn(manager);
    RecipeHolder<IMeltingRecipe> holder = new RecipeHolder<>(Identifier.parse("tconstruct:test_machine_output"), recipe);
    when(manager.getRecipeFor(TinkerRecipeTypes.MELTING.get(), module, level)).thenReturn(Optional.of(holder));
    machine.setLevel(level);
    machine.getMeltingInventory().setStackInSlot(0, stack.copy());

    assertThat(module.getOreRate()).isSameAs(foundry ? Config.COMMON.foundryOreRate : Config.COMMON.smelteryOreRate);
    assertThat(module.getRequiredTime()).as("machine can heat this recipe").isPositive();
    module.heatItem(recipe.getTemperature(module), module.getRequiredTime());
    module.heatItem(recipe.getTemperature(module), 1);
    assertThat(machine.getMeltingInventory().getStackInSlot(0).isEmpty()).as("successful melting consumes the input").isTrue();

    Map<Fluid,Integer> expected = new LinkedHashMap<>();
    expected.put(BuiltInRegistries.FLUID.get(Identifier.parse(result.get("fluid").getAsString())), amount);
    if (foundry) {
      for (FluidStack byproduct : byproducts) {
        if (!byproduct.isEmpty()) expected.merge(byproduct.getFluid(), byproduct.getAmount(), Integer::sum);
      }
    }
    assertThat(machine.getTank().getContained()).as("%s total output", foundry ? "foundry" : "smeltery")
      .isEqualTo(expected.values().stream().mapToInt(Integer::intValue).sum());
    assertThat(machine.getTank().getFluids()).hasSize(expected.size());
    for (Map.Entry<Fluid,Integer> entry : expected.entrySet()) {
      assertThat(machine.getTank().getFluids()).anySatisfy(fluid -> {
        assertThat(fluid.getFluid()).isSameAs(entry.getKey());
        assertThat(fluid.getAmount()).isEqualTo(entry.getValue());
      });
    }
  }

  private static void assertMelting(IMeltingRecipe recipe, JsonObject json, String type) {
    JsonObject result = json.getAsJsonObject("result");
    JsonArray byproducts = json.has("byproducts") ? json.getAsJsonArray("byproducts") : new JsonArray();
    boolean material = type.equals("tconstruct:material_melting");
    boolean damaged = type.equals("tconstruct:damagable_melting");
    boolean ore = type.equals("tconstruct:ore_melting");
    for (IOreRate oreRate : List.of(Config.COMMON.melterOreRate, Config.COMMON.smelteryOreRate, Config.COMMON.foundryOreRate)) {
      for (int variation = 0; variation < (material || damaged ? 3 : 1); variation++) {
        int cost = material ? new int[] {1, 2, 9}[variation] : 1;
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        if (material) {
          var part = switch (variation) {
            case 0 -> TinkerToolParts.fakeIngot.get();
            case 1 -> TinkerToolParts.pickHead.get();
            default -> TinkerToolParts.fakeStorageBlockItem.get();
          };
          MaterialCastingLookup.registerItemCost(part, cost);
          stack = new ItemStack(part.asItem());
        }
        if (damaged) stack.setDamageValue(new int[] {0, stack.getMaxDamage() / 2, stack.getMaxDamage() - 1}[variation]);
        Input input = new Input(stack, oreRate);
        int amount = result.get("amount").getAsInt() * cost;
        if (ore) amount = oreRate.applyOreBoost(rate(json, OreRateType.NONE), amount);
        int unit = result.has("unit_size") ? result.get("unit_size").getAsInt() : 1;
        if (damaged) amount = damagedAmount(result, 1, stack);
        assertFluid(recipe.getOutput(input), result, amount);
        List<FluidStack> actualByproducts = recipe.getByproducts(input);
        assertThat(actualByproducts).hasSize(byproducts.size());
        for (int i = 0; i < byproducts.size(); i++) {
          JsonObject byproduct = byproducts.get(i).getAsJsonObject();
          int byproductAmount = byproduct.get("amount").getAsInt() * cost;
          if (ore) byproductAmount = Config.COMMON.foundryByproductRate.applyOreBoost(rate(byproduct, rate(json, OreRateType.NONE)), byproductAmount);
          if (damaged) byproductAmount = damagedAmount(byproduct, unit, stack);
          assertFluid(actualByproducts.get(i), byproduct, byproductAmount);
        }
        if (oreRate == Config.COMMON.smelteryOreRate) {
          assertMachineOutputs(recipe, stack, result, amount, actualByproducts, false);
        } else if (oreRate == Config.COMMON.foundryOreRate) {
          assertMachineOutputs(recipe, stack, result, amount, actualByproducts, true);
        }
        if (!material && variation == 0) {
          List<List<FluidStack>> displayed = ((MeltingRecipe)recipe).getOutputWithByproducts(oreRate);
          assertThat(displayed).hasSize(1 + actualByproducts.size());
          assertFluid(displayed.getFirst().getFirst(), result, amount);
          for (int i = 0; i < actualByproducts.size(); i++) {
            assertFluid(displayed.get(i + 1).getFirst(), byproducts.get(i).getAsJsonObject(), actualByproducts.get(i).getAmount());
          }
        }
      }
    }
  }

  private static void assertAlloy(AlloyRecipe recipe, JsonObject json) {
    FoundryBlockEntity parent = new FoundryBlockEntity(net.minecraft.core.BlockPos.ZERO, TinkerSmeltery.foundryController.get().defaultBlockState());
    parent.getTank().setCapacity(100000);
    SmelteryAlloyTank tank = new SmelteryAlloyTank(parent.getTank());
    tank.setTemperature(recipe.getTemperature());
    int total = 0;
    int catalysts = 0;
    for (AlloyRecipe.AlloyIngredient ingredient : recipe.getInputs()) {
      FluidStack fluid = ingredient.fluid().getFluids().getFirst().copy();
      total += fluid.getAmount();
      if (ingredient.catalyst()) catalysts += fluid.getAmount();
      tank.fill(fluid);
    }
    assertThat(parent.getTank().getContained()).isEqualTo(total);
    assertThat(recipe.canPerform(tank)).isTrue();
    recipe.performRecipe(tank);
    int amount = json.getAsJsonObject("result").get("amount").getAsInt();
    assertThat(parent.getTank().getContained()).isEqualTo(amount + catalysts);
    assertFluid(parent.getTank().getFluids().getLast(), json.getAsJsonObject("result"), amount);
    assertFluid(recipe.getOutput(), json.getAsJsonObject("result"), amount);
  }

  @Test
  void everyShippedFurnaceOutputMatchesItsDataBeforeAndAfterSync() throws IOException {
    RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
    List<String> failures = new ArrayList<>();
    int checked = 0;
    Path recipes = DATA.resolve("tconstruct/recipe");
    try (var files = Files.walk(recipes)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
        JsonObject source = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        String type = source.get("type").getAsString();
        if (!MELTING.contains(type) && !OTHER.contains(type)) continue;
        try {
          JsonObject json = source.deepCopy();
          if (json.has("result") && !type.startsWith("minecraft:")) resolveFluids(json.get("result"), source);
          if (json.has("byproducts")) resolveFluids(json.get("byproducts"), source);
          if (type.equals("tconstruct:alloy")) resolveFluids(json.get("inputs"), source);
          if (type.equals("tconstruct:melting_fuel")) resolveFluids(json.get("fluid"), source);
          if (MELTING.contains(type) && !type.equals("tconstruct:material_melting")) {
            json.add("ingredient", JsonParser.parseString("{\"item\":\"minecraft:iron_pickaxe\"}"));
          }
          if (type.equals("tconstruct:entity_melting")) json.add("entity", JsonParser.parseString("{\"type\":\"minecraft:pig\"}"));
          Recipe<?> parsed = Recipe.CODEC.parse(ops, json).getOrThrow();
          Identifier id = Identifier.fromNamespaceAndPath("tconstruct", recipes.relativize(file).toString().replace('\\', '/').replace(".json", ""));
          for (Recipe<?> recipe : List.of(parsed, roundTrip(id, parsed))) {
            if (recipe instanceof IMeltingRecipe melting) {
              assertMelting(melting, json, type);
            } else if (recipe instanceof AlloyRecipe alloy) {
              assertAlloy(alloy, json);
            } else if (recipe instanceof EntityMeltingRecipe entity) {
              assertThat(entity.matches(EntityType.PIG)).isTrue();
              assertFluid(entity.getOutput(), json.getAsJsonObject("result"), json.getAsJsonObject("result").get("amount").getAsInt());
              assertFluid(entity.getOutput(null), json.getAsJsonObject("result"), json.getAsJsonObject("result").get("amount").getAsInt());
            } else if (recipe instanceof AbstractCookingRecipe cooking) {
              ItemStack output = cooking.assemble(new SingleRecipeInput(ItemStack.EMPTY), access);
              JsonObject result = json.getAsJsonObject("result");
              assertThat(BuiltInRegistries.ITEM.getKey(output.getItem()).toString()).isEqualTo(result.get("id").getAsString());
              assertThat(output.getCount()).isEqualTo(result.has("count") ? result.get("count").getAsInt() : 1);
              assertThat(ItemStack.matches(output, cooking.getResultItem(access))).isTrue();
            } else if (recipe instanceof MeltingFuel fuel) {
              assertThat(fuel.getRate()).isEqualTo(json.get("rate").getAsInt());
              assertThat(fuel.getTemperature()).isEqualTo(json.get("temperature").getAsInt());
              if (json.has("fluid")) {
                assertThat(fuel.getDuration()).isEqualTo(json.get("duration").getAsInt());
                for (FluidStack fluid : fuel.getInputs()) assertThat(fuel.getAmount(fluid.getFluid())).isEqualTo(fluid.getAmount());
              }
            } else {
              throw new AssertionError("Unhandled furnace recipe " + recipe.getClass());
            }
          }
          checked++;
        } catch (RuntimeException | AssertionError e) {
          failures.add(recipes.relativize(file) + ": " + e.getMessage());
        }
      }
    }
    assertThat(failures).as("furnace output audit (%s checked)", checked).isEmpty();
    assertThat(checked).as("all shipped furnace recipes, including optional compat").isGreaterThanOrEqualTo(917);
  }
}
