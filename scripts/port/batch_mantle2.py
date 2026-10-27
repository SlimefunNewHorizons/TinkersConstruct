"""Second batch of Mantle fixes for 1.21.11: commands, loadables, recipes, ingredients, registration and combat."""
import re

from portio import edit, read, write

M = 'src/main/java/slimeknights/mantle/'


def add_import(path, *imports):
  text = read(path)
  nl = '\r\n' if '\r\n' in text else '\n'
  for imp in imports:
    line = 'import ' + imp + ';'
    if line not in text:
      text = re.sub(r'^(import [^\n]*\n)', line + nl + r'\1', text, count=1, flags=re.M)
  write(path, text)


# combat: hurt returns void, sweep particles and walk distance moved in 1.21.2+
edit(M + 'util/CombatHelper.java', [
  ('(player.walkDist - player.walkDistO) < player.getSpeed()', 'player.getKnownMovement().horizontalDistanceSqr() < Mth.square(player.getSpeed() * 2.5)'),
  ('hit = target.hurt(damageSource, damage);', 'hit = target.hurtOrSimulate(damageSource, damage);'),
  ('living.hurt(player.damageSources().playerAttack(player), sweepDamage);', 'living.hurtOrSimulate(player.damageSources().playerAttack(player), sweepDamage);'),
  ('            player.sweepAttack();\n', '''            if (player.level() instanceof ServerLevel serverLevel) {
              double x = -Mth.sin(player.getYRot() * TO_RADIAN);
              double z = Mth.cos(player.getYRot() * TO_RADIAN);
              serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + x, player.getY(0.5), player.getZ() + z, 0, x, 0.0, z, 0.0);
            }
'''),
])
edit(M + 'block/fluid/BurningLiquidBlock.java', [('entity.hurt(entity.damageSources().lava(), damage)', 'entity.hurtOrSimulate(entity.damageSources().lava(), damage)')])
edit(M + 'util/TranslationHelper.java', [('addOptionalTooltip(stack.getDescriptionId() + ".tooltip", tooltip);', 'addOptionalTooltip(stack.getItem().getDescriptionId() + ".tooltip", tooltip);')])

# registry lookups
edit(M + 'command/argument/TagSourceArgument.java', [('ResourceKey::location', 'ResourceKey::identifier'), ('      .registry(key)\n', '      .lookup(key)\n')])
edit(M + 'command/RegistryArgument.java', [('.registry(ResourceKey.createRegistryKey(id))', '.lookup(ResourceKey.createRegistryKey(id))')])
edit(M + 'data/loadable/Loadables.java', [('ResourceKey::location', 'ResourceKey::identifier')])

# pack formats have a major and minor version since 1.21.9
edit(M + 'command/GeneratePackHelper.java', [('''    JsonObject pack = new JsonObject();
    pack.addProperty("description", description);
    pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(packType));
    meta.add("pack", pack);''', '''    // pack formats have a major and minor version since 1.21.9, let the vanilla codec write them
    PackMetadataSection pack = new PackMetadataSection(Component.literal(description), new InclusiveRange<>(SharedConstants.getCurrentVersion().packVersion(packType)));
    meta.add("pack", PackMetadataSection.forPackType(packType).codec().encodeStart(JsonOps.INSTANCE, pack).getOrThrow());''')])
add_import(M + 'command/GeneratePackHelper.java', 'net.minecraft.server.packs.metadata.pack.PackMetadataSection', 'net.minecraft.util.InclusiveRange', 'net.minecraft.network.chat.Component', 'com.mojang.serialization.JsonOps')

edit(M + 'command/MantleCommand.java', [
  ('context.getSource().getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE)', 'context.getSource().getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElementIds().map(ResourceKey::identifier)'),
  ('context.getSource().getRecipeNames()', 'context.getSource().getServer().getRecipeManager().getRecipes().stream().map(holder -> holder.id().identifier())'),
])
add_import(M + 'command/MantleCommand.java', 'net.minecraft.resources.ResourceKey')

# recipes no longer expose results or ingredients directly
edit(M + 'command/RemoveRecipesCommand.java', [
  ('    RegistryAccess access = level.registryAccess();\n', '    ContextMap displayContext = SlotDisplayContext.fromLevel(level);\n'),
  ('removeResult.test(recipe.getResultItem(access).getItem())', 'recipe.display().stream().flatMap(display -> display.result().resolveForStacks(displayContext).stream()).anyMatch(result -> removeResult.test(result.getItem()))'),
  ('recipes.add(holder.id());', 'recipes.add(holder.id().identifier());'),
  ('            for (Ingredient ingredient : recipe.getIngredients()) {', '            // shortcut: only placeable recipes list their ingredients, custom recipes are skipped by input filters; upgrade once recipes expose inputs again\n            for (Ingredient ingredient : recipe.placementInfo().ingredients()) {'),
])
add_import(M + 'command/RemoveRecipesCommand.java', 'net.minecraft.util.context.ContextMap', 'net.minecraft.world.item.crafting.display.SlotDisplayContext')
edit(M + 'command/TagsForCommand.java', [('ProjectileUtil.getEntityHitResult(source.getLevel(), player, start, direction, bb, e -> true)', 'ProjectileUtil.getEntityHitResult(source.getLevel(), player, start, direction, bb, e -> true, 0.3F)')])

# loadables
edit(M + 'data/loadable/common/IngredientLoadable.java', [
  ('''  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    return (this == ALLOW_EMPTY ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY)
      .parse(JSON_OPS, element).getOrThrow(JsonParseException::new);
  }''', '''  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    // vanilla ingredients cannot be empty since 1.21.2, keep supporting the legacy empty array
    if (element.isJsonArray() && element.getAsJsonArray().isEmpty()) {
      if (this == DISALLOW_EMPTY) {
        throw new JsonSyntaxException("Ingredient cannot be empty");
      }
      return IngredientHelper.EMPTY;
    }
    Ingredient ingredient = Ingredient.CODEC.parse(JSON_OPS, element).getOrThrow(JsonParseException::new);
    if (this == DISALLOW_EMPTY && IngredientHelper.isEmpty(ingredient)) {
      throw new JsonSyntaxException("Ingredient cannot be empty");
    }
    return ingredient;
  }'''),
  ('''    if (object.isEmpty() && this == DISALLOW_EMPTY) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    return (this == ALLOW_EMPTY ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY)
      .encodeStart(JSON_OPS, object).getOrThrow(JsonParseException::new);''', '''    if (IngredientHelper.isEmpty(object)) {
      if (this == DISALLOW_EMPTY) {
        throw new IllegalArgumentException("Ingredient cannot be empty");
      }
      return new JsonArray();
    }
    return Ingredient.CODEC.encodeStart(JSON_OPS, object).getOrThrow(JsonParseException::new);'''),
])
add_import(M + 'data/loadable/common/IngredientLoadable.java', 'com.google.gson.JsonArray', 'com.google.gson.JsonSyntaxException', 'slimeknights.mantle.recipe.helper.IngredientHelper')
edit(M + 'data/loadable/common/NBTLoadable.java', [('TagParser.parseTag(', 'TagParser.parseCompoundFully(')])
edit(M + 'data/predicate/item/ItemPredicate.java', [('simple(Item::hasCraftingRemainingItem)', 'simple(item -> !item.getCraftingRemainder().isEmpty())')])

# items and inventories
edit(M + 'item/AbstractBookItem.java', [('inventory.items.contains(stack) || inventory.offhand.contains(stack)', 'inventory.getNonEquipmentItems().contains(stack) || player.getOffhandItem() == stack')])
edit(M + 'client/book/BookHelper.java', [('.copyTag().getCompound(BOOK_COMPOUND).getCompound(BOOK_DATA_COMPOUND)', '.copyTag().getCompoundOrEmpty(BOOK_COMPOUND).getCompoundOrEmpty(BOOK_DATA_COMPOUND)')])
add_import(M + 'inventory/WrapperSlot.java', 'javax.annotation.Nullable')
edit(M + 'block/entity/InventoryBlockEntity.java', [('public void startOpen(Player player) {}', 'public void startOpen(ContainerUser user) {}'), ('public void stopOpen(Player player) {}', 'public void stopOpen(ContainerUser user) {}')])
add_import(M + 'block/entity/InventoryBlockEntity.java', 'net.minecraft.world.entity.ContainerUser')

# cooking recipes: accessors renamed, result is no longer exposed by vanilla
for name in ['Blasting', 'Campfire', 'Smelting', 'Smoking']:
  path = M + 'recipe/cooking/' + name + 'ResultRecipe.java'
  edit(path, [
    ('AbstractCookingRecipe::getCookingTime', 'AbstractCookingRecipe::cookingTime'),
    ('r -> r.ingredient)', 'r -> r.input())'),
    ('  @Override\n  public ItemStack getResultItem(', '  public ItemStack getResultItem('),
  ])
edit(M + 'recipe/cooking/CookingResultRecipe.java', [('AbstractCookingRecipe::getExperience', 'AbstractCookingRecipe::experience')])
edit(M + 'recipe/helper/LoadableRecipeSerializer.java', [('Recipe::getGroup', 'Recipe::group')])

# shaped recipes: patterns hold optional ingredients, result field is opened by the access transformer
edit(M + 'recipe/crafting/ShapedRetexturedRecipe.java', [
  ('NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification,', 'List<Optional<Ingredient>> ingredients, ItemStack result, boolean showNotification,'),
  ('''    super(original.getGroup(), original.category(), original.pattern,
      original.getResultItem(RegistryAccess.EMPTY).copy(), original.showNotification());''', '''    super(original.group(), original.category(), original.pattern, original.result.copy(), original.showNotification());'''),
  ('return RetexturedHelper.setTexture(getResultItem(access).copy(), Block.byItem(texture));', 'return RetexturedHelper.setTexture(result.copy(), Block.byItem(texture));'),
  ('Ingredient.CODEC_NONEMPTY', 'Ingredient.CODEC'),
])
add_import(M + 'recipe/crafting/ShapedRetexturedRecipe.java', 'java.util.List')
edit(M + 'recipe/crafting/ShapedFallbackRecipe.java', [
  ('NonNullList<Ingredient> ingredients, ItemStack output, List<Identifier> alternatives)', 'List<Optional<Ingredient>> ingredients, ItemStack output, List<Identifier> alternatives)'),
  ('super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY).copy(), base.showNotification());', 'super(base.group(), base.category(), base.pattern, base.result.copy(), base.showNotification());'),
  ('buffer::writeResourceLocation', 'buffer::writeIdentifier'),
])

# custom ingredients list item holders and their display since 1.21.2
edit(M + 'recipe/ingredient/ItemIngredient.java', [('''  @Override
  public Stream<ItemStack> getItems() {''', '''  @Override
  public Stream<Holder<Item>> items() {
    return getItems().map(ItemStack::getItemHolder).distinct();
  }

  @Override
  public SlotDisplay display() {
    return new SlotDisplay.Composite(getItems().<SlotDisplay>map(SlotDisplay.ItemStackSlotDisplay::new).toList());
  }

  /** Gets the stacks to display for this ingredient */
  public Stream<ItemStack> getItems() {''')])
add_import(M + 'recipe/ingredient/ItemIngredient.java', 'net.minecraft.world.item.crafting.display.SlotDisplay')
edit(M + 'recipe/ingredient/FluidContainerIngredient.java', [('''  @Override
  public Stream<ItemStack> getItems() {
    return display == null ? Stream.empty() : java.util.Arrays.stream(IngredientHelper.getItems(display));
  }''', '''  @Override
  public Stream<Holder<Item>> items() {
    return display == null ? Stream.empty() : display.items();
  }

  @Override
  public SlotDisplay display() {
    return display == null ? SlotDisplay.Empty.INSTANCE : display.display();
  }''')])
add_import(M + 'recipe/ingredient/FluidContainerIngredient.java', 'net.minecraft.core.Holder', 'net.minecraft.world.item.Item', 'net.minecraft.world.item.crafting.display.SlotDisplay')

# registration: block entity types lost data fixer types, entity types need keys, spawn eggs take the type as a component
TYPE_METHOD = re.compile(r'\r?\n  /\*\*\r?\n   \* Gets the data fixer type[^/]*/\r?\n  @Nullable\r?\n  private Type<\?> getType\(String name\) \{\r?\n.*?\r?\n  \}\r?\n', re.S)
for path in [M + 'registration/adapter/BlockEntityTypeRegistryAdapter.java', M + 'registration/deferred/BlockEntityTypeDeferredRegister.java']:
  text = read(path)
  text = TYPE_METHOD.sub('', text, count=1)
  text = text.replace('BlockEntityType.Builder.<T>of(factory, block).build(getType(name))', 'new BlockEntityType<T>(factory, block)')
  text = text.replace('BlockEntityType.Builder.<T>of(factory, block.get()).build(getType(name))', 'new BlockEntityType<T>(factory, block.get())')
  text = text.replace(', getType(name))', ')')
  write(path, text)
edit(M + 'registration/adapter/EntityTypeRegistryAdapter.java', [('builder.build(resourceName(name))', 'builder.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(resourceName(name))))')])
add_import(M + 'registration/adapter/EntityTypeRegistryAdapter.java', 'net.minecraft.resources.ResourceKey', 'net.minecraft.core.registries.Registries', 'net.minecraft.resources.Identifier')
edit(M + 'registration/deferred/EntityTypeDeferredRegister.java', [
  ('sup.get().build(resourceName(name))', 'sup.get().build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(resourceName(name))))'),
  ('() -> new DeferredSpawnEggItem(object, primary, secondary, new Item.Properties())', '() -> new SpawnEggItem(new Item.Properties().spawnEgg(object.get()))'),
  ('import net.neoforged.neoforge.common.DeferredSpawnEggItem;', 'import net.minecraft.world.item.SpawnEggItem;'),
  ('   * @param primary    Primary egg color\n   * @param secondary  Secondary egg color\n', '   * @param primary    Primary egg color, unused since spawn eggs use textures in 1.21.5\n   * @param secondary  Secondary egg color, unused since spawn eggs use textures in 1.21.5\n'),
])
add_import(M + 'registration/deferred/EntityTypeDeferredRegister.java', 'net.minecraft.resources.ResourceKey', 'net.minecraft.core.registries.Registries', 'net.minecraft.resources.Identifier')
edit(M + 'registration/adapter/ItemRegistryAdapter.java', [
  ('import net.minecraft.client.data.models.blockstates.PropertyDispatch.TriFunction;', 'import com.mojang.datafixers.util.Function3;'),
  ('import net.neoforged.neoforge.common.DeferredSpawnEggItem;\n', ''),
  ('TriFunction<Item.Properties', 'Function3<Item.Properties'),
  ('burnableSignItem = SignItem::new;', 'burnableSignItem = (props, standing, wall) -> new SignItem(standing, wall, props);'),
  ('return register(new DeferredSpawnEggItem(type, primary, secondary, new Properties()), baseName + "_spawn_egg");', '// spawn eggs use textures since 1.21.5, so the colors are unused\n    return register(new SpawnEggItem(new Properties().spawnEgg(type.get())), baseName + "_spawn_egg");'),
])
edit(M + 'registration/adapter/BlockRegistryAdapter.java', [
  ('.dropsLike(standingSign)', '.overrideLootTable(standingSign.getLootTable()).overrideDescription(standingSign.getDescriptionId())'),
  ('.dropsLike(hangingSign)', '.overrideLootTable(hangingSign.getLootTable()).overrideDescription(hangingSign.getDescriptionId())'),
])
edit(M + 'registration/deferred/BlockDeferredRegister.java', [
  ('.lootFrom(standingSign)', '.overrideLootTable(standingSign.get().getLootTable()).overrideDescription(standingSign.get().getDescriptionId())'),
  ('.lootFrom(hangingSign)', '.overrideLootTable(hangingSign.get().getLootTable()).overrideDescription(hangingSign.get().getDescriptionId())'),
  ('new SignItem(signProps, standing, wall)', 'new SignItem(standing, wall, signProps)'),
])
edit(M + 'recipe/ingredient/EntityIngredient.java', [
  ('''      // vanilla eggs live in SpawnEggItem.byId while DeferredSpawnEggItem.deferredOnlyById
      // only knows modded eggs registered through its constructor; check both so vanilla
      // entities get a usable JEI focus link
      eggs = getDisplay().stream().map(type -> {
        SpawnEggItem egg = SpawnEggItem.byId(type.type);
        if (egg == null) {
          egg = DeferredSpawnEggItem.deferredOnlyById(type.type);
        }
        return new ItemStack(Objects.requireNonNullElse(egg, Items.AIR));
      }).toList();''', '''      eggs = getDisplay().stream().map(type -> new ItemStack(Objects.requireNonNullElse(SpawnEggItem.byId(type.type), Items.AIR))).toList();'''),
  ('import net.neoforged.neoforge.common.DeferredSpawnEggItem;\n', ''),
])
print('ok')
