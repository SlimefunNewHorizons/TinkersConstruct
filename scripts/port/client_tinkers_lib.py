"""Ports the Tinkers library client helpers (book, materials) to 1.21.11"""
from portio import add_import, edit

L = 'src/main/java/slimeknights/tconstruct/library/client/'

edit(L + 'book/TinkerBook.java', [(
  'BookLoader.registerGsonTypeAdapter(Component.class, new Component.SerializerAdapter(net.minecraft.core.RegistryAccess.EMPTY));',
  'BookLoader.registerGsonTypeAdapter(Component.class, (JsonDeserializer<Component>) (json, type, context) -> ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new));')])
add_import(L + 'book/TinkerBook.java', 'com.google.gson.JsonDeserializer', 'com.google.gson.JsonParseException', 'com.mojang.serialization.JsonOps', 'net.minecraft.network.chat.ComponentSerialization')

edit(L + 'book/content/AbstractMaterialContent.java', [
  ('.flatMap(recipe -> Arrays.stream(recipe.getIngredient().getItems()))', '.flatMap(recipe -> IngredientHelper.getDisplayItems(recipe.getIngredient()).stream())'),
  ('Component.translatable(CAST_FROM, firstFluid.getDisplayName())', 'Component.translatable(CAST_FROM, firstFluid.getHoverName())'),
  ('Component.translatable(COMPOSITE_FROM, firstFluid.getDisplayName(),', 'Component.translatable(COMPOSITE_FROM, firstFluid.getHoverName(),'),
])
add_import(L + 'book/content/AbstractMaterialContent.java', 'slimeknights.mantle.recipe.helper.IngredientHelper')

edit(L + 'book/content/ContentPageIconList.java', [('wordWrapHeight(subText, BookScreen.PAGE_WIDTH)', 'wordWrapHeight(FormattedText.of(subText), BookScreen.PAGE_WIDTH)')])
add_import(L + 'book/content/ContentPageIconList.java', 'net.minecraft.network.chat.FormattedText')

edit(L + 'book/content/ContentTool.java', [
  ('Item item = BuiltInRegistries.ITEM.get(Identifier.parse(this.toolName));', 'Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(this.toolName));'),
  ('''      Recipe<?> recipe = Optional.ofNullable(Minecraft.getInstance().level)
                                                 .flatMap(world -> {
                                                   RegistryAccess access = world.registryAccess();
                                                   return RecipeAccessHelper.getRecipeMap(world).values().stream()
                                                        .map(holder -> holder.value())
                                                        .filter(r -> r.getType() == RecipeType.CRAFTING && r.getResultItem(access).getItem() == tool.asItem())
                                                        .findFirst();
                                                 })
                                                 .orElse(null);
      if (recipe != null) {
        // parts is just the items in the recipe
        this.parts = recipe.getIngredients().stream().map(ingredient -> ItemStackList.of(ingredient.getItems())).collect(Collectors.toList());
''', '''      Recipe<?> recipe = Optional.ofNullable(Minecraft.getInstance().level)
                                                 .flatMap(world -> RecipeAccessHelper.getRecipeMap(world).values().stream()
                                                        .map(holder -> holder.value())
                                                        .filter(r -> r instanceof ShapedRecipe shaped ? shaped.result.is(tool.asItem()) : r instanceof ShapelessRecipe shapeless && shapeless.result.is(tool.asItem()))
                                                        .findFirst())
                                                 .orElse(null);
      if (recipe != null) {
        // parts is just the items in the recipe
        if (recipe instanceof ShapedRecipe shaped) {
          this.parts = shaped.getIngredients().stream().map(ingredient -> ItemStackList.of(ingredient.map(IngredientHelper::getItems).orElse(new ItemStack[0]))).collect(Collectors.toList());
        } else {
          this.parts = ((ShapelessRecipe) recipe).ingredients.stream().map(ingredient -> ItemStackList.of(IngredientHelper.getItems(ingredient))).collect(Collectors.toList());
        }
'''),
  ('          partBuilder.add(ItemStackList.of(ingredient.getItems()));', '          partBuilder.add(ItemStackList.of(IngredientHelper.getItems(ingredient)));'),
])
add_import(L + 'book/content/ContentTool.java', 'slimeknights.mantle.recipe.helper.IngredientHelper', 'net.minecraft.world.item.crafting.ShapelessRecipe', 'net.minecraft.world.item.ItemStack')

edit(L + 'book/elements/CycleRecipeElement.java', [('this.button.onPress();', 'this.button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));')])
add_import(L + 'book/elements/CycleRecipeElement.java', 'net.minecraft.client.input.KeyEvent', 'com.mojang.blaze3d.platform.InputConstants')
edit(L + 'book/elements/PageIconLinkElement.java', [('    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);\n', ''), ('import com.mojang.blaze3d.systems.RenderSystem;\n', '')])
edit(L + 'materials/MaterialRenderInfo.java', [('new Material(InventoryMenu.BLOCK_ATLAS,', 'new Material(TextureAtlas.LOCATION_BLOCKS,')])
add_import(L + 'materials/MaterialRenderInfo.java', 'net.minecraft.client.renderer.texture.TextureAtlas')
print('ok')
