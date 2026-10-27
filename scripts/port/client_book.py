"""Ports Mantle book data classes to 1.21.11"""
from portio import add_import, edit

B = 'src/main/java/slimeknights/mantle/client/book/'

edit(B + 'data/content/ContentCrafting.java', [
  ('''      Recipe<?> recipe = level.getRecipeManager().byKey(recipeId).map(holder -> holder.value()).orElse(null);
      if (recipe instanceof CraftingRecipe) {
        if(grid_size.equalsIgnoreCase("auto")) {
          if(recipe.canCraftInDimensions(2, 2)) {''', '''      // recipes only reach the client if their type is synced through RecipeAccessHelper
      Recipe<?> recipe = RecipeAccessHelper.byKey(level, recipeId).map(holder -> holder.value()).orElse(null);
      if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
        if(grid_size.equalsIgnoreCase("auto")) {
          if(fits(recipe, 2, 2)) {'''),
  ('''        if (!recipe.canCraftInDimensions(w, h)) {''', '''        if (!fits(recipe, w, h)) {'''),
  ('''        result = IngredientData.getItemStackData(recipe.getResultItem(level.registryAccess()));

        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        if (recipe instanceof ShapedRecipe shaped) {
          grid = new IngredientData[shaped.getHeight()][shaped.getWidth()];

          for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
              grid[y][x] = IngredientData.getItemStackData(NonNullList.of(ItemStack.EMPTY, ingredients.get(x + y * grid[y].length).getItems()));
            }
          }

          return;
        }

        grid = new IngredientData[h][w];
        for (int i = 0; i < ingredients.size(); i++) {
          grid[i / h][i % w] = IngredientData.getItemStackData(NonNullList.of(ItemStack.EMPTY, ingredients.get(i).getItems()));
        }''', '''        if (recipe instanceof ShapedRecipe shaped) {
          result = IngredientData.getItemStackData(shaped.result);
          List<Optional<Ingredient>> ingredients = shaped.getIngredients();
          grid = new IngredientData[shaped.getHeight()][shaped.getWidth()];

          for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
              grid[y][x] = getData(ingredients.get(x + y * grid[y].length));
            }
          }

          return;
        }

        ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
        result = IngredientData.getItemStackData(shapeless.result);
        List<Ingredient> ingredients = shapeless.ingredients;
        grid = new IngredientData[h][w];
        for (int i = 0; i < ingredients.size(); i++) {
          grid[i / h][i % w] = getData(Optional.of(ingredients.get(i)));
        }'''),
])
edit(B + 'data/content/ContentCrafting.java', [('''  public void load() {''', '''  /** Checks if the recipe fits in the given grid */
  private static boolean fits(Recipe<?> recipe, int width, int height) {
    if (recipe instanceof ShapedRecipe shaped) {
      return shaped.getWidth() <= width && shaped.getHeight() <= height;
    }
    return ((ShapelessRecipe) recipe).ingredients.size() <= width * height;
  }

  /** Gets the display data for an ingredient slot */
  private static IngredientData getData(Optional<Ingredient> ingredient) {
    return IngredientData.getItemStackData(NonNullList.of(ItemStack.EMPTY, ingredient.map(IngredientHelper::getItems).orElse(new ItemStack[0])));
  }

  @Override
  public void load() {''')], required=False)
add_import(B + 'data/content/ContentCrafting.java', 'net.minecraft.world.item.crafting.ShapelessRecipe', 'slimeknights.mantle.recipe.helper.RecipeAccessHelper',
           'slimeknights.mantle.recipe.helper.IngredientHelper', 'java.util.List', 'java.util.Optional')

edit(B + 'data/content/ContentSmelting.java', [
  ('''      Recipe<?> recipe = level.getRecipeManager().byKey(recipeId).map(holder -> holder.value()).orElse(null);

      if (recipe instanceof AbstractCookingRecipe) {
        this.input = IngredientData.getItemStackData(NonNullList.of(ItemStack.EMPTY, recipe.getIngredients().get(0).getItems()));
        this.cookTime = ((AbstractCookingRecipe) recipe).getCookingTime();
        this.result = IngredientData.getItemStackData(recipe.getResultItem(level.registryAccess()));''', '''      Recipe<?> recipe = RecipeAccessHelper.byKey(level, recipeId).map(holder -> holder.value()).orElse(null);

      if (recipe instanceof AbstractCookingRecipe cooking) {
        this.input = IngredientData.getItemStackData(NonNullList.of(ItemStack.EMPTY, IngredientHelper.getItems(cooking.input())));
        this.cookTime = cooking.cookingTime();
        this.result = IngredientData.getItemStackData(cooking.assemble(new SingleRecipeInput(ItemStack.EMPTY), level.registryAccess()));'''),
])
add_import(B + 'data/content/ContentSmelting.java', 'net.minecraft.world.item.crafting.SingleRecipeInput', 'slimeknights.mantle.recipe.helper.RecipeAccessHelper',
           'slimeknights.mantle.recipe.helper.IngredientHelper')
print('ok')
