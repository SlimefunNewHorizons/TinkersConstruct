"""Tinkers 1.21.11 fixes, batch 1: gadgets, fluids, json, modifiers, ingredients."""
import re

from portio import edit, read, write

T = 'src/main/java/slimeknights/tconstruct/'


def add_import(path, *imports):
  text = read(path)
  nl = '\r\n' if '\r\n' in text else '\n'
  for imp in imports:
    line = 'import ' + imp + ';'
    if line not in text:
      text = re.sub(r'^(import [^\n]*\n)', line + nl + r'\1', text, count=1, flags=re.M)
  write(path, text)


edit(T + 'common/Sounds.java', [('sound.sound.getLocation()', 'sound.sound.location()')])
edit(T + 'library/json/variable/StatLoadable.java', [('name = ((Item) value).getDescription();', 'name = ((Item) value).getName();')])
edit(T + 'library/materials/definition/MaterialVariantId.java', [('Identifier.isAllowedInResourceLocation(ch)', 'Identifier.isAllowedInIdentifier(ch)')])
edit(T + 'library/modifiers/ModifierEntry.java', [('tag.getInt(TAG_AMOUNT), tag.getInt(TAG_NEEDED)', 'tag.getIntOr(TAG_AMOUNT, 0), tag.getIntOr(TAG_NEEDED, 0)')])
edit(T + 'library/modifiers/hook/interaction/InteractionSource.java', [('case HUMANOID_ARMOR, ANIMAL_ARMOR -> ARMOR;', 'case HUMANOID_ARMOR, ANIMAL_ARMOR, SADDLE -> ARMOR;')])
# cooldowns are keyed by stack since 1.21.2, a plain stack of the item shares the item's cooldown group
edit(T + 'library/modifiers/hook/interaction/GeneralInteractionModifierHook.java', [('player.getCooldowns().addCooldown(tool, getDrawtime(', 'player.getCooldowns().addCooldown(new ItemStack(tool.getItem()), getDrawtime(')])
add_import(T + 'library/modifiers/hook/interaction/GeneralInteractionModifierHook.java', 'net.minecraft.world.item.ItemStack')
edit(T + 'library/modifiers/fluid/block/BreakBlockFluidEffect.java', [('state.onDestroyedByPlayer(world, pos, player, true, world.getFluidState(pos))', 'state.onDestroyedByPlayer(world, pos, player, ItemStack.EMPTY, true, world.getFluidState(pos))')])
add_import(T + 'library/modifiers/fluid/block/BreakBlockFluidEffect.java', 'net.minecraft.world.item.ItemStack')
for p in ['library/modifiers/fluid/block/PotionCloudFluidEffect.java', 'library/modifiers/fluid/entity/PotionFluidEffect.java']:
  edit(T + p, [('.forEachEffect(effects::add);', '.forEachEffect(effects::add, 1.0f);')])
edit(T + 'library/modifiers/fluid/entity/PotionFluidEffect.java', [('''            if (action.execute()) {
              target.invulnerableTime = 0;
              effect.value().applyInstantenousEffect(directSource, attacker, target, instance.getAmplifier(), used * scale);''', '''            if (action.execute() && target.level() instanceof ServerLevel server) {
              target.invulnerableTime = 0;
              effect.value().applyInstantenousEffect(server, directSource, attacker, target, instance.getAmplifier(), used * scale);''')])
add_import(T + 'library/modifiers/fluid/entity/PotionFluidEffect.java', 'net.minecraft.server.level.ServerLevel')

# potions: names come from the contents, instant effects need the server level
edit(T + 'library/utils/PotionUtils.java', [
  ('  /** Gets potion contents from an item, including legacy custom data during migration. */', '''  /** Gets the translation key for the potion contents, replacement for the removed {@code Potion#getName(Optional, String)} */
  public static String getName(PotionContents contents, String prefix) {
    return prefix + contents.potion().map(potion -> potion.value().name()).orElse("empty");
  }

  /** Gets potion contents from an item, including legacy custom data during migration. */'''),
  ('BuiltInRegistries.POTION.getHolder(id).orElse(null)', 'BuiltInRegistries.POTION.get(id).orElse(null)'),
  ('''    Optional<Integer> color = tag != null && tag.contains("CustomPotionColor")
      ? Optional.of(tag.getInt("CustomPotionColor")) : Optional.empty();''', '''    Optional<Integer> color = tag != null ? tag.getInt("CustomPotionColor") : Optional.empty();'''),
  ('new PotionContents(Optional.ofNullable(potion), color, List.of())', 'new PotionContents(Optional.ofNullable(potion), color, List.of(), Optional.empty())'),
  ('return PotionContents.getColor(potion);', 'return new PotionContents(potion).getColor();'),
  ('contents.forEachEffect(effects::add);', 'contents.forEachEffect(effects::add, 1.0f);'),
])
for p in ['fluids/fluids/PotionFluidType.java', 'fluids/item/PotionBucketItem.java']:
  text = read(T + p)
  text = re.sub(r'Potion\.getName\((PotionUtils\.getPotionContents\(stack\))\.potion\(\), ', r'PotionUtils.getName(\1, ', text)
  write(T + p, text)
edit(T + 'fluids/item/PotionBucketItem.java', [('effect.getEffect().value().applyInstantenousEffect(player, player, living, effect.getAmplifier(), POTION_SCALE);', 'effect.getEffect().value().applyInstantenousEffect((ServerLevel) level, player, player, living, effect.getAmplifier(), POTION_SCALE);')])
add_import(T + 'fluids/item/PotionBucketItem.java', 'net.minecraft.server.level.ServerLevel')

# tag loading: element lookup takes a required flag, load and build are separate steps
for p, lookup in [('library/materials/definition/MaterialManager.java', 'new TagLoader<>(id -> getMaterial(new MaterialId(id)), TAG_FOLDER)'), ('library/modifiers/ModifierManager.java', 'new TagLoader<>(id -> {')]:
  text = read(T + p)
  text = text.replace(lookup, lookup.replace('(id ->', '((id, required) ->'))
  text = re.sub(r'tagLoader\.loadAndBuild\((\w+)\)', r'tagLoader.build(tagLoader.load(\1))', text)
  write(T + p, text)
edit(T + 'library/utils/GenericTagUtil.java', [('Map<Identifier,Collection<T>> map)', 'Map<Identifier,? extends Collection<T>> map)')])
edit(T + 'library/modifiers/ModifierManager.java', [('FMLLoader.isProduction()', 'FMLEnvironment.isProduction()')])
add_import(T + 'library/modifiers/ModifierManager.java', 'net.neoforged.fml.loading.FMLEnvironment')

# 1.21.5: item sub predicates became data component predicates
edit(T + 'library/json/predicate/tool/ToolStackItemPredicate.java', [
  ('import net.minecraft.advancements.critereon.ItemSubPredicate;', 'import net.minecraft.core.component.DataComponentGetter;\nimport net.minecraft.core.component.predicates.DataComponentPredicate;'),
  ('implements ItemSubPredicate {', 'implements DataComponentPredicate {'),
  ('public static final ItemSubPredicate.Type<ToolStackItemPredicate> TYPE = new ItemSubPredicate.Type<>(CODEC);', 'public static final DataComponentPredicate.Type<ToolStackItemPredicate> TYPE = new DataComponentPredicate.ConcreteType<>(CODEC);'),
  ('''  public boolean matches(ItemStack stack) {
    // tag check is important to prevent accidently modifying the NBT of non-tools
    return stack.is(Items.MODIFIABLE) && predicate.matches(ToolStack.from(stack));''', '''  public boolean matches(DataComponentGetter components) {
    // item predicates pass the stack, tag check is important to prevent accidently modifying the NBT of non-tools
    return components instanceof ItemStack stack && stack.is(Items.MODIFIABLE) && predicate.matches(ToolStack.from(stack));'''),
])
edit(T + 'common/TinkerModule.java', [
  ('import net.minecraft.advancements.critereon.ItemSubPredicate;', 'import net.minecraft.core.component.predicates.DataComponentPredicate;'),
  ('SynchronizedDeferredRegister<ItemSubPredicate.Type<?>> ITEM_SUB_PREDICATES = SynchronizedDeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE,', 'SynchronizedDeferredRegister<DataComponentPredicate.Type<?>> ITEM_SUB_PREDICATES = SynchronizedDeferredRegister.create(Registries.DATA_COMPONENT_PREDICATE_TYPE,'),
])
edit(T + 'tools/TinkerTools.java', [('RegistryObject<net.minecraft.advancements.critereon.ItemSubPredicate.Type<?>> toolStackPredicate', 'RegistryObject<net.minecraft.core.component.predicates.DataComponentPredicate.Type<?>> toolStackPredicate')])

# gadgets
edit(T + 'gadgets/entity/FancyItemFrameEntity.java', [
  ('blockPosition().relative(direction.getOpposite())', 'blockPosition().relative(getDirection().getOpposite())'),
  ('Util.createTraceResult(behind, direction, false)', 'Util.createTraceResult(behind, getDirection(), false)'),
  ('InteractionResult result = itemResult.result();', 'InteractionResult result = itemResult;'),
  ('this.direction.get3DDataValue()', 'this.getDirection().get3DDataValue()'),
])
edit(T + 'gadgets/item/FancyItemFrameItem.java', [('CustomData tag = stack.get(DataComponents.ENTITY_DATA);', 'TypedEntityData<EntityType<?>> tag = stack.get(DataComponents.ENTITY_DATA);')])
add_import(T + 'gadgets/item/FancyItemFrameItem.java', 'net.minecraft.world.item.component.TypedEntityData')
edit(T + 'gadgets/item/PiggyBackPackItem.java', [('target.startRiding(toRide, true)', 'target.startRiding(toRide, true, true)')])
edit(T + 'gadgets/item/ShootProjectileDispenserBehavior.java', [('entity.create(level);', 'entity.create(level, EntitySpawnReason.DISPENSER);')])
add_import(T + 'gadgets/item/ShootProjectileDispenserBehavior.java', 'net.minecraft.world.entity.EntitySpawnReason')

# instruments are a datapack registry since 1.21.2, the component holds an either holder
edit(T + 'library/recipe/ingredient/InstrumentIngredient.java', [
  ('''    Holder<Instrument> holder = stack.get(DataComponents.INSTRUMENT);
    if (holder != null) {
      if (this.instrument != null) {
        return holder.unwrapKey().map(key -> key.equals(this.instrument)).orElse(false);
      }
      assert this.ignore != null;
      // must be a valid instrument outside the ignored tag
      return !holder.is(this.ignore);
    }''', '''    InstrumentComponent component = stack.get(DataComponents.INSTRUMENT);
    if (component != null) {
      if (this.instrument != null) {
        return component.instrument().key().map(key -> key.equals(this.instrument)).orElse(false);
      }
      assert this.ignore != null;
      // must be a valid instrument outside the ignored tag
      return component.unwrap(ValueIOHelper.registries(null)).map(holder -> !holder.is(this.ignore)).orElse(false);
    }'''),
  ('BuiltInRegistries.INSTRUMENT.getHolder(instrument).ifPresent(holder -> stack.set(DataComponents.INSTRUMENT, holder));', 'stack.set(DataComponents.INSTRUMENT, new InstrumentComponent(instrument));'),
])
add_import(T + 'library/recipe/ingredient/InstrumentIngredient.java', 'net.minecraft.world.item.component.InstrumentComponent', 'slimeknights.mantle.util.ValueIOHelper')
edit(T + 'library/modifiers/modules/display/MeleeInstrumentModule.java', [
  ('instrument = BuiltInRegistries.INSTRUMENT.get(key);', 'instrument = attacker.level().registryAccess().lookupOrThrow(Registries.INSTRUMENT).getValue(key);'),
  ('instrument = BuiltInRegistries.INSTRUMENT.getTag(tag)', 'instrument = attacker.level().registryAccess().lookupOrThrow(Registries.INSTRUMENT).get(tag)'),
])
add_import(T + 'library/modifiers/modules/display/MeleeInstrumentModule.java', 'net.minecraft.core.registries.Registries')

# nested ingredients delegate display to the nested ingredient
edit(T + 'library/recipe/ingredient/NestedIngredient.java', [('''  @Override
  public Stream<ItemStack> getItems() {
    return Arrays.stream(IngredientHelper.getItems(nested));
  }''', '''  @Override
  public Stream<Holder<Item>> items() {
    return nested.items();
  }

  @Override
  public SlotDisplay display() {
    return nested.display();
  }''')])
add_import(T + 'library/recipe/ingredient/NestedIngredient.java', 'net.minecraft.core.Holder', 'net.minecraft.world.item.Item', 'net.minecraft.world.item.crafting.display.SlotDisplay')
edit(T + 'library/recipe/ingredient/NoContainerIngredient.java', [
  ('!stack.hasCraftingRemainingItem()', 'stack.getCraftingRemainder().isEmpty()'),
  ('return of(Ingredient.of(stacks));', 'return of(Ingredient.of(java.util.Arrays.stream(stacks).map(ItemStack::getItem)));'),
])
print('ok')
