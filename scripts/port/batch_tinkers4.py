"""Tinkers 1.21.11 fixes, batch 4: crafting recipes, smeltery, tables, items, utils."""
import re

from portio import edit, read, write
from remove_methods import remove

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


def replace_all(path, pairs):
  text = read(path)
  for old, new in pairs:
    text = text.replace(old, new)
  write(path, text)


def remove_methods(path, *names):
  text = read(path)
  for name in names:
    text, _ = remove(text, name)
  write(path, text)


def void_to_boolean(path, method):
  """Item#releaseUsing returns whether it did something since 1.21.2"""
  text = read(path)
  m = re.search(r'public void ' + method + r'\(', text)
  start = text.index('{', m.end())
  depth, i = 0, start
  while True:
    if text[i] == '{':
      depth += 1
    elif text[i] == '}':
      depth -= 1
      if depth == 0:
        break
    i += 1
  body = text[start:i].replace('return;', 'return false;')
  nl = '\r\n' if '\r\n' in text else '\n'
  body = body.rstrip() + nl + '    return true;' + nl + '  '
  write(path, text[:m.start()] + 'public boolean ' + method + '(' + text[m.end():start] + body + text[i:])


# shaped and shapeless material recipes: results and ingredient lists are opened by the access transformer
for p in ['library/recipe/material/ShapedMaterialRecipe.java', 'library/recipe/material/ShapedMaterialsRecipe.java']:
  text = read(T + p)
  text = text.replace('NonNullList<Ingredient> ingredients, ItemStack result', 'List<Optional<Ingredient>> ingredients, ItemStack result')
  text = re.sub(r'super\((\w+)\.getGroup\(\), \1\.category\(\), \1\.pattern, \1\.getResultItem\(RegistryAccess\.EMPTY\)\.copy\(\), \1\.showNotification\(\)\);', r'super(\1.group(), \1.category(), \1.pattern, \1.result.copy(), \1.showNotification());', text)
  write(T + p, text)
edit(T + 'library/recipe/material/ShapedMaterialRecipe.java', [('''      for (Ingredient ingredient : getIngredients()) {
        if (ingredient.getCustomIngredient() instanceof MaterialValueIngredient materialValue) {''', '''      for (Optional<Ingredient> ingredient : getIngredients()) {
        if (ingredient.isPresent() && ingredient.get().getCustomIngredient() instanceof MaterialValueIngredient materialValue) {''')])
replace_all(T + 'library/recipe/material/ShapelessMaterialsRecipe.java', [
  ('super(recipe.getGroup(), recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY).copy(), recipe.getIngredients());', 'super(recipe.group(), recipe.category(), recipe.result.copy(), recipe.ingredients);'),
  ('    return getIngredients();', '    return ingredients;'),
  ('inventory, getIngredients(), partCount', 'inventory, ingredients, partCount'),
  ('parts > vanilla.getIngredients().size()', 'parts > vanilla.ingredients.size()'),
])
edit(T + 'library/recipe/modifiers/adding/OverslimeCraftingTableRecipe.java', [('if (stack.hasCraftingRemainingItem()) {', 'if (!stack.getCraftingRemainder().isEmpty()) {')])
for p in ['library/recipe/modifiers/adding/OverslimeCraftingTableRecipe.java', 'library/recipe/tinkerstation/repairing/ModifierRepairCraftingRecipe.java', 'tables/recipe/CraftingTableRepairKitRecipe.java']:
  remove_methods(T + p, 'canCraftInDimensions')
edit(T + 'library/recipe/tinkerstation/ITinkerStationRecipe.java', [('''  /** @deprecated use {@link #updateInputs(LazyToolStack, IMutableTinkerStationContainer, boolean)} */
  @Override
  @Deprecated
  default NonNullList<ItemStack> getRemainingItems''', '''  /** @deprecated use {@link #updateInputs(LazyToolStack, IMutableTinkerStationContainer, boolean)} */
  @Deprecated
  default NonNullList<ItemStack> getRemainingItems''')])

# tools and items
edit(T + 'library/tools/helper/ToolAttackUtil.java', [('float baseDamage = context.baseDamage;', 'float baseDamage = context.getBaseDamage();')])
for p in ['library/tools/item/ranged/ModifiableBowItem.java', 'library/tools/item/ranged/ModifiableCrossbowItem.java']:
  void_to_boolean(T + p, 'releaseUsing')
edit(T + 'library/tools/item/ranged/ModifiableCrossbowItem.java', [('heldStack.getItem().appendHoverText(heldStack, Item.TooltipContext.of(player.level()), nestedTooltip, tooltipFlag);', 'heldStack.getItem().appendHoverText(heldStack, Item.TooltipContext.of(player.level()), TooltipDisplay.DEFAULT, nestedTooltip::add, tooltipFlag);')])
add_import(T + 'library/tools/item/ranged/ModifiableCrossbowItem.java', 'net.minecraft.world.item.component.TooltipDisplay')
remove_methods(T + 'library/tools/part/MaterialItem.java', 'verifyComponentsAfterLoad')
remove_methods(T + 'library/tools/part/block/MaterialBlockItem.java', 'verifyComponentsAfterLoad')
# description ids are final since 1.21.2, the per stack name comes from getName
remove_methods(T + 'fluids/item/PotionBucketItem.java', 'getDescriptionId')
for p in ['smeltery/item/CopperCanItem.java', 'smeltery/item/TankItem.java']:
  text = read(T + p)
  text, _ = remove(text, 'hasCraftingRemainingItem')
  text = text.replace('public ItemStack getCraftingRemainingItem(ItemStack stack) {', 'public ItemStack getCraftingRemainder(ItemStack stack) {')
  text = text.replace('if (hasCraftingRemainingItem(stack)) {', 'if (getFluid(stack) != Fluids.EMPTY) {')
  write(T + p, text)
edit(T + 'smeltery/item/TankItem.java', [('return nbt.getCompound(FLUID_TAG).isPresent() ? nbt.getCompound(FLUID_TAG) : nbt;', 'return nbt.getCompound(FLUID_TAG).orElse(nbt);')])

# utils
text = read(T + 'library/utils/TeleportHelper.java')
text = re.sub(r'  private static final Set<Relative> PACKET_FLAGS = [^\n]*\n', '', text)
text = text.replace('playerMP.connection.teleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), playerMP.getYRot(), playerMP.getXRot(), PACKET_FLAGS);', 'playerMP.connection.teleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), playerMP.getYRot(), playerMP.getXRot());')
write(T + 'library/utils/TeleportHelper.java', text)
replace_all(T + 'library/utils/Util.java', [('net.minecraft.Util.makeDescriptionId(', 'net.minecraft.util.Util.makeDescriptionId('), ('direction.getNormal().equals(offset)', 'direction.getUnitVec3i().equals(offset)')])
edit(T + 'shared/AchievementEvents.java', [('MinecraftServer server = playerMP.getServer();', 'MinecraftServer server = playerMP.level().getServer();')])
edit(T + 'shared/item/CheeseBlockItem.java', [('living.spawnAtLocation(cheese);', 'living.spawnAtLocation((ServerLevel) living.level(), cheese);')])
add_import(T + 'shared/item/CheeseBlockItem.java', 'net.minecraft.server.level.ServerLevel')
# loot table drops are resolved from the properties since 1.21.2
edit(T + 'shared/block/GlowBlock.java', [('    super(properties);\n    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));\n    this.drops = BuiltInLootTables.EMPTY;', '    super(properties.noLootTable());\n    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));'),
                                        ('level.getBlockState(placedOn).getOcclusionShape(level, pos)', 'level.getBlockState(placedOn).getOcclusionShape()')])

# smeltery
edit(T + 'smeltery/block/FaucetBlock.java', [('private static final Vector3f RED = new Vector3f(1.0F, 0.0F, 0.0F);', 'private static final int RED = 0xFF0000;')])
edit(T + 'smeltery/block/FluidCannonBlock.java', [
  ('implements IFluidCannon, Equipable, IBlockEntityRemovalHandler {', 'implements IFluidCannon, IBlockEntityRemovalHandler {'),
  ('import net.minecraft.world.item.Equipable;\n', ''),
])
remove_methods(T + 'smeltery/block/FluidCannonBlock.java', 'getEquipmentSlot')
edit(T + 'smeltery/TinkerSmeltery.java', [
  ('    Function<Block, BlockItem> lanternItem = b -> new TankItem(b, ITEM_PROPS, false);', '    Function<Block, BlockItem> lanternItem = b -> new TankItem(b, ITEM_PROPS, false);\n    // cannons are worn as hats, blocks lost Equipable for the equippable component in 1.21.2\n    Function<Block, BlockItem> cannonItem = b -> new TankItem(b, new Item.Properties().component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build()), true);'),
  ('new FluidCannonBlock(seared, FluidType.BUCKET_VOLUME * 2, 1.0f, 1.1f, 6.0f), tankItem);', 'new FluidCannonBlock(seared, FluidType.BUCKET_VOLUME * 2, 1.0f, 1.1f, 6.0f), cannonItem);'),
  ('new FluidCannonBlock(scorched, FluidType.BUCKET_VOLUME * 2, 2.0f, 1.5f, 7.0f), tankItem);', 'new FluidCannonBlock(scorched, FluidType.BUCKET_VOLUME * 2, 2.0f, 1.5f, 7.0f), cannonItem);'),
  ('new KnightMetalFluidCannonBlock(seared, FluidType.BUCKET_VOLUME * 4, 1.5f, 3.0f, 6.0f), tankItem);', 'new KnightMetalFluidCannonBlock(seared, FluidType.BUCKET_VOLUME * 4, 1.5f, 3.0f, 6.0f), cannonItem);'),
])
add_import(T + 'smeltery/TinkerSmeltery.java', 'net.minecraft.core.component.DataComponents', 'net.minecraft.world.item.equipment.Equippable', 'net.minecraft.world.entity.EquipmentSlot', 'net.minecraft.world.item.Item')
edit(T + 'smeltery/block/component/SearedTintedGlassBlock.java', [('return world.getMaxLightLevel();', 'return LightEngine.MAX_LEVEL;')])
add_import(T + 'smeltery/block/component/SearedTintedGlassBlock.java', 'net.minecraft.world.level.lighting.LightEngine')
edit(T + 'smeltery/block/entity/ChannelBlockEntity.java', [('byte[] nbtFlowing = nbt.getByteArray(TAG_IS_FLOWING);', 'byte[] nbtFlowing = nbt.getByteArray(TAG_IS_FLOWING).orElse(new byte[0]);')])
replace_all(T + 'smeltery/block/entity/FaucetBlockEntity.java', [('ValueIOHelper.saveStack(drained, registries)', 'ValueIOHelper.saveFluid(drained, registries)'), ('compound.getCompound(TAG_DRAINED)', 'compound.getCompoundOrEmpty(TAG_DRAINED)'), ('compound.getCompound(TAG_RENDER_FLUID)', 'compound.getCompoundOrEmpty(TAG_RENDER_FLUID)')])
replace_all(T + 'smeltery/block/entity/tank/SmelteryTank.java', [('ValueIOHelper.saveStack(liquid, registries)', 'ValueIOHelper.saveFluid(liquid, registries)')])
replace_all(T + 'smeltery/block/entity/tank/CastingFluidHandler.java', [('nbt.getCompound(TAG_FLUID)', 'nbt.getCompoundOrEmpty(TAG_FLUID)')])
edit(T + 'smeltery/block/entity/component/DuctBlockEntity.java', [('''  public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
    super.handleUpdateTag(tag, registries);''', '''  public void handleUpdateTag(ValueInput input) {
    super.handleUpdateTag(input);''')])
add_import(T + 'smeltery/block/entity/component/DuctBlockEntity.java', 'net.minecraft.world.level.storage.ValueInput')
edit(T + 'smeltery/block/entity/component/SmelteryInputOutputBlockEntity.java', [('level.blockUpdated(worldPosition, getBlockState().getBlock());', 'level.updateNeighborsAt(worldPosition, getBlockState().getBlock());')])
remove_methods(T + 'smeltery/block/entity/component/TankBlockEntity.java', 'registries')
for p in ['smeltery/block/entity/inventory/HeaterItemHandler.java', 'smeltery/block/entity/module/SolidFuelModule.java']:
  text = read(T + p)
  text = text.replace('stack.getBurnTime(TinkerRecipeTypes.FUEL.get())', 'stack.getBurnTime(TinkerRecipeTypes.FUEL.get(), FUEL_VALUES)')
  write(T + p, text)
edit(T + 'smeltery/block/entity/module/EntityMeltingModule.java', [('return !entity.isInvulnerableTo(entity.fireImmune() ? smelteryMagic() : smelteryHeat())', 'return !(entity.level() instanceof ServerLevel server && entity.isInvulnerableTo(server, entity.fireImmune() ? smelteryMagic() : smelteryHeat()))')])
add_import(T + 'smeltery/block/entity/module/EntityMeltingModule.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'smeltery/block/entity/module/MeltingModule.java', [('nbt = (CompoundTag)stack.save(registries, nbt);', 'nbt = (CompoundTag)ValueIOHelper.saveStack(stack, registries);')])
add_import(T + 'smeltery/block/entity/module/MeltingModule.java', 'slimeknights.mantle.util.ValueIOHelper')
replace_all(T + 'smeltery/block/entity/module/MeltingModuleInventory.java', [('nbt.getByte(TAG_SIZE) & 255', 'nbt.getByteOr(TAG_SIZE, (byte) 0) & 255'), ('item.getByte(TAG_SLOT) & 255', 'item.getByteOr(TAG_SLOT, (byte) 0) & 255')])
edit(T + 'smeltery/block/entity/multiblock/MultiblockStructureData.java', [('list.add(NbtUtils.writeBlockPos(pos.subtract(basePos)));', 'list.add(BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos.subtract(basePos)).getOrThrow());')])
add_import(T + 'smeltery/block/entity/multiblock/MultiblockStructureData.java', 'net.minecraft.nbt.NbtOps')
edit(T + 'smeltery/block/entity/tank/ProxyItemTank.java', [('Item craftRemainingItem = stack.getItem().getCraftingRemainingItem();', 'ItemStack craftRemainder = stack.getItem().getCraftingRemainder();\n    Item craftRemainingItem = craftRemainder.isEmpty() ? null : craftRemainder.getItem();')])
edit(T + 'smeltery/item/ScaledFluidTank.java', [
  ('  @Override\n  public FluidTank readFromNBT(', '  /** Reads the tank from NBT, NeoForge tanks moved to value IO in 1.21.6 */\n  public FluidTank readFromNBT('),
  ('  @Override\n  public CompoundTag writeToNBT(', '  /** Writes the tank to NBT, NeoForge tanks moved to value IO in 1.21.6 */\n  public CompoundTag writeToNBT('),
  ('Fluid legacyFluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);', 'Fluid legacyFluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(id);'),
  ('fluid = new FluidStack(legacyFluid, nbt.getInt("Amount"));', 'fluid = new FluidStack(legacyFluid, nbt.getIntOr("Amount", 0));'),
  ('return fluid.isEmpty() ? nbt : (CompoundTag)fluid.save(registries, nbt);', 'if (!fluid.isEmpty()) {\n      nbt.merge((CompoundTag)ValueIOHelper.saveFluid(fluid, registries));\n    }\n    return nbt;'),
])
edit(T + 'smeltery/item/TankItem.java', [('nbt.put(NBTTags.TANK, tank.writeToNBT(RegistryAccessUtil.BUILTIN, new CompoundTag()));', 'nbt.put(NBTTags.TANK, ValueIOHelper.writeTank(tank, RegistryAccessUtil.BUILTIN, new CompoundTag()));')])

# tables
edit(T + 'tables/block/TinkersChestBlock.java', [('new DyedItemColor(te.getColor(), true)', 'new DyedItemColor(te.getColor())')])
edit(T + 'tables/block/entity/inventory/IChestItemHandler.java', [('extends IItemHandlerModifiable, INBTSerializable<CompoundTag>, IScalingContainer {', 'extends IItemHandlerModifiable, IScalingContainer {')])
text = read(T + 'tables/block/entity/inventory/IChestItemHandler.java')
text = re.sub(r'import net\.neoforged\.neoforge\.common\.util\.INBTSerializable;\r?\n', '', text)
text = re.sub(r'import net\.minecraft\.nbt\.CompoundTag;\r?\n', '', text)
write(T + 'tables/block/entity/inventory/IChestItemHandler.java', text)
edit(T + 'tables/block/entity/chest/AbstractChestBlockEntity.java', [
  ('''    CompoundTag handlerNBT = itemHandler.serializeNBT(registries);
    tags.put(KEY_ITEMS, handlerNBT.getListOrEmpty(KEY_ITEMS));''', '''    // same layout as the legacy item handler NBT, NeoForge handlers moved to value IO in 1.21.6
    ListTag items = new ListTag();
    for (int i = 0; i < itemHandler.getSlots(); i++) {
      ItemStack stack = itemHandler.getStackInSlot(i);
      if (!stack.isEmpty()) {
        CompoundTag item = (CompoundTag) ValueIOHelper.saveStack(stack, registries);
        item.putByte("Slot", (byte) i);
        items.add(item);
      }
    }
    tags.put(KEY_ITEMS, items);'''),
  ('''    CompoundTag handlerNBT = new CompoundTag();
    handlerNBT.put(KEY_ITEMS, tags.getListOrEmpty(KEY_ITEMS));
    itemHandler.deserializeNBT(registries, handlerNBT);''', '''    for (int i = 0; i < itemHandler.getSlots(); i++) {
      itemHandler.setStackInSlot(i, ItemStack.EMPTY);
    }
    ListTag items = tags.getListOrEmpty(KEY_ITEMS);
    for (int i = 0; i < items.size(); i++) {
      CompoundTag item = items.getCompoundOrEmpty(i);
      int slot = item.getByteOr("Slot", (byte) 0) & 255;
      if (slot < itemHandler.getSlots()) {
        itemHandler.setStackInSlot(slot, ValueIOHelper.parseStack(registries, item));
      }
    }'''),
])
add_import(T + 'tables/block/entity/chest/AbstractChestBlockEntity.java', 'net.minecraft.nbt.ListTag', 'net.minecraft.world.item.ItemStack', 'slimeknights.mantle.util.ValueIOHelper')
edit(T + 'tables/block/entity/table/CraftingStationBlockEntity.java', [
  ('new UpdateCraftingRecipePacket(this.worldPosition, this.lastRecipe.id())', 'new UpdateCraftingRecipePacket(this.worldPosition, this.lastRecipe.id().identifier())'),
  ('public void updateRecipe(net.minecraft.resources.ResourceLocation id, CraftingRecipe recipe) {', 'public void updateRecipe(Identifier id, CraftingRecipe recipe) {'),
])
add_import(T + 'tables/block/entity/table/CraftingStationBlockEntity.java', 'net.minecraft.resources.Identifier')
edit(T + 'tables/block/entity/table/TinkerStationBlockEntity.java', [('lastRecipeId = holder == null ? null : holder.id();', 'lastRecipeId = holder == null ? null : holder.id().identifier();')])
# slot icons are a single sprite since 1.21.2
for p in ['tables/menu/ModifierWorktableContainerMenu.java', 'tables/menu/TinkerStationContainerMenu.java', 'tables/menu/slot/ArmorSlot.java']:
  replace_all(T + p, [('setBackground(InventoryMenu.BLOCK_ATLAS, ', 'setBackground(')])
edit(T + 'tables/menu/slot/CraftingResultSlot.java', [('stack.onCraftedBy(this.player.level(), this.player, this.removeCount);', 'stack.onCraftedBy(this.player, this.removeCount);')])

# entities
edit(T + 'tools/entity/CustomFireball.java', [('public boolean hurt(DamageSource pSource, float pAmount) {', 'public boolean hurtServer(ServerLevel level, DamageSource pSource, float pAmount) {')])
replace_all(T + 'tools/entity/CustomFireball.java', [('if (target.hurt(source, getDamage())', 'if (target.hurtOrSimulate(source, getDamage())')])
add_import(T + 'tools/entity/CustomFireball.java', 'net.minecraft.server.level.ServerLevel')
gadget = T + 'gadgets/entity/shuriken/ShurikenEntityBase.java'
replace_all(gadget, [
  ('public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, double x, double y, double z, Level worldIn) {\n    super(type, x, y, z, worldIn);', 'public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, double x, double y, double z, Level worldIn, ItemStack stack) {\n    super(type, x, y, z, worldIn, stack);'),
  ('public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, LivingEntity livingEntityIn, Level worldIn) {\n    super(type, livingEntityIn, worldIn);', 'public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, LivingEntity livingEntityIn, Level worldIn, ItemStack stack) {\n    super(type, livingEntityIn, worldIn, stack);'),
])
add_import(gadget, 'net.minecraft.world.item.ItemStack')

# fluid effects and modules
edit(T + 'library/modifiers/fluid/block/BlockInteractFluidEffect.java', [('SoundEvents.ITEM_BREAK, SoundSource.BLOCKS', 'SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS'), ('InteractionResult result = itemResult.result();', 'InteractionResult result = itemResult;')])
replace_all(T + 'library/modifiers/modules/combat/ConditionalPowerModule.java', [('persistentData.getDouble(AMMO_MULTIPLIER).isPresent()', 'persistentData.contains(AMMO_MULTIPLIER, TagUtil.TAG_ANY_NUMERIC)'), ('persistentData.getDouble(BOW_MULTIPLIER).isPresent()', 'persistentData.contains(BOW_MULTIPLIER, TagUtil.TAG_ANY_NUMERIC)')])
add_import(T + 'library/modifiers/modules/combat/ConditionalPowerModule.java', 'slimeknights.tconstruct.library.utils.TagUtil')
print('ok')
