"""Tinkers 1.21.11 fixes, batch 5: armor trims, recipe map fallback, misc entity and item overrides."""
import re

from portio import edit, read, write

T = 'src/main/java/slimeknights/tconstruct/'
M = 'src/main/java/slimeknights/mantle/'


def add_import(path, *imports):
  text = read(path)
  nl = '\r\n' if '\r\n' in text else '\n'
  for imp in imports:
    line = 'import ' + imp + ';'
    if line not in text:
      text = re.sub(r'^(import [^\n]*\n)', line + nl + r'\1', text, count=1, flags=re.M)
  write(path, text)


# without a level, server side callers need the server's recipes
edit(M + 'recipe/helper/RecipeAccessHelper.java', [('''    if (level instanceof ServerLevel server) {
      return server.recipeAccess().recipeMap();
    }
    return clientRecipes;''', '''    if (level instanceof ServerLevel server) {
      return server.recipeAccess().recipeMap();
    }
    if (level == null && EffectiveSide.get().isServer()) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
        return server.getRecipeManager().recipeMap();
      }
    }
    return clientRecipes;''')])
add_import(M + 'recipe/helper/RecipeAccessHelper.java', 'net.minecraft.server.MinecraftServer', 'net.neoforged.neoforge.server.ServerLifecycleHooks', 'net.neoforged.fml.util.thread.EffectiveSide')
edit(T + 'library/recipe/TinkerRecipeTypes.java', [('RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING);', 'RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING, RecipeType.SMITHING);')])

# armor trims: templates are defined by smithing trim recipes since 1.21.5
edit(T + 'tools/recipe/ArmorTrimRecipe.java', [
  ('  /** Match for the trim item finding method */', '''  /** Finds the trim pattern for the template, templates are defined by smithing trim recipes since 1.21.5 */
  @Nullable
  private static Holder<TrimPattern> findPattern(ItemStack template) {
    for (RecipeHolder<SmithingRecipe> holder : RecipeAccessHelper.byType(null, RecipeType.SMITHING)) {
      if (holder.value() instanceof SmithingTrimRecipe trim && trim.templateIngredient().map(ingredient -> ingredient.test(template)).orElse(false)) {
        return trim.pattern;
      }
    }
    return null;
  }

  /** Gets all trim templates */
  private static List<ItemStack> getTemplates() {
    return RecipeAccessHelper.byType(null, RecipeType.SMITHING).stream()
      .flatMap(holder -> holder.value() instanceof SmithingTrimRecipe trim ? trim.templateIngredient().stream() : Stream.empty())
      .flatMap(ingredient -> IngredientHelper.getDisplayItems(ingredient).stream())
      .toList();
  }

  /** Match for the trim item finding method */'''),
  ('        if (stack.is(ItemTags.TRIM_TEMPLATES)) {', '        if (findPattern(stack) != null) {'),
  ('Reference<TrimMaterial> material = TrimMaterials.getFromIngredient(access, trimItems.material).orElse(null);', 'Holder<TrimMaterial> material = TrimMaterials.getFromIngredient(access, trimItems.material).orElse(null);'),
  ('    Reference<TrimPattern> pattern = null;', '    Holder<TrimPattern> pattern = null;'),
  ('      pattern = TrimPatterns.getFromTemplate(access, trimItems.template).orElse(null);', '      pattern = findPattern(trimItems.template);'),
  ('material.key().identifier().toString()', 'material.unwrapKey().orElseThrow().identifier().toString()'),
  ('pattern.key().identifier().toString()', 'pattern.unwrapKey().orElseThrow().identifier().toString()'),
  ('''      List<ItemStack> trims = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, ItemTags.TRIM_TEMPLATES)
                                            .map(ItemStack::new).toList();''', '''      List<ItemStack> trims = getTemplates();'''),
  ('.map(material -> new DisplayRecipe(id, toolInputs, trims, material))', '.map(material -> new DisplayRecipe(id, toolInputs, trims, material, access))'),
  ('public DisplayRecipe(Identifier id, List<ItemStack> tools, List<ItemStack> trim, Reference<TrimMaterial> holder) {', 'public DisplayRecipe(Identifier id, List<ItemStack> tools, List<ItemStack> trim, Reference<TrimMaterial> holder, RegistryAccess access) {'),
  ('this.material = List.of(new ItemStack(material.ingredient().value()));', '''// materials are defined by an item component since 1.21.5
      this.material = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, ItemTags.TRIM_MATERIALS).map(ItemStack::new)
        .filter(stack -> TrimMaterials.getFromIngredient(access, stack).filter(found -> found.is(holder)).isPresent()).toList();'''),
])
add_import(T + 'tools/recipe/ArmorTrimRecipe.java', 'net.minecraft.core.Holder', 'net.minecraft.world.item.crafting.RecipeHolder', 'net.minecraft.world.item.crafting.RecipeType', 'net.minecraft.world.item.crafting.SmithingRecipe', 'net.minecraft.world.item.crafting.SmithingTrimRecipe',
           'slimeknights.mantle.recipe.helper.RecipeAccessHelper', 'slimeknights.mantle.recipe.helper.IngredientHelper', 'java.util.stream.Stream', 'java.util.List', 'javax.annotation.Nullable')

# entities
edit(T + 'tools/entity/FluidEffectProjectile.java', [('''    double x = packet.getXa();
    double y = packet.getYa();
    double z = packet.getZa();''', '''    Vec3 movement = packet.getMovement();
    double x = movement.x;
    double y = movement.y;
    double z = movement.z;''')])
add_import(T + 'tools/entity/FluidEffectProjectile.java', 'net.minecraft.world.phys.Vec3')
edit(T + 'tools/entity/ThrownShuriken.java', [('    this.checkInsideBlocks();', '    this.applyEffectsFromBlocks();')])

# items and containers
edit(T + 'tools/item/ModifiableSwordItem.java', [('''  public boolean canAttackBlock(BlockState state, Level worldIn, BlockPos pos, Player player) {
    return !player.isCreative();''', '''  public boolean canDestroyBlock(ItemStack stack, BlockState state, Level worldIn, BlockPos pos, LivingEntity entity) {
    return !(entity instanceof Player player && player.isCreative());''')])
add_import(T + 'tools/item/ModifiableSwordItem.java', 'net.minecraft.world.item.ItemStack', 'net.minecraft.world.entity.LivingEntity')
edit(T + 'tables/block/entity/inventory/CraftingContainerWrapper.java', [('public void fillStackedContents(StackedContents helper) {', 'public void fillStackedContents(StackedItemContents helper) {')])
add_import(T + 'tables/block/entity/inventory/CraftingContainerWrapper.java', 'net.minecraft.world.entity.player.StackedItemContents')
edit(T + 'tables/item/AnvilBlockItem.java', [
  ('return Component.translatable(this.getDescriptionId(stack));', 'return Component.translatable(this.getDescriptionId());'),
  ('this.getBlock().appendHoverText(stack, context, tooltip, flag);', 'if (this.getBlock() instanceof IBlockTooltip block) {\n          block.appendHoverText(stack, context, display, tooltip::add, flag);\n        }'),
  ('public String getCreatorModId(ItemStack stack) {', 'public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {'),
])
add_import(T + 'tables/item/AnvilBlockItem.java', 'slimeknights.mantle.block.IBlockTooltip', 'net.minecraft.core.HolderLookup')
print('ok')
