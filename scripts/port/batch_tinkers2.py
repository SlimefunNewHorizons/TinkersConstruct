"""Tinkers 1.21.11 fixes, batch 2: tool library, NBT views, attachments, potions, utils."""
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


def sub_all(path, pattern, repl):
  text = read(path)
  write(path, re.sub(pattern, repl, text))


# a rule wrapped the cast into the helper call, move it back out
for p in ['library/tools/capability/fluid/ToolTankHelper.java', 'library/tools/capability/inventory/InventoryModule.java']:
  sub_all(T + p, r'ValueIOHelper\.save(Fluid|Stack)\(\(CompoundTag\)(\w+), ', r'(CompoundTag)ValueIOHelper.save\1(\2, ')
edit(T + 'library/tools/capability/fluid/ToolTankHelper.java', [('ValueIOHelper.parseFluid(slimeknights.tconstruct.library.utils.RegistryAccessUtil.BUILTIN, nbt.getCompound(key))', 'ValueIOHelper.parseFluid(slimeknights.tconstruct.library.utils.RegistryAccessUtil.BUILTIN, nbt.getCompoundOrEmpty(key))')])
edit(T + 'library/tools/capability/inventory/InventoryModule.java', [
  ('if (compound.getInt(TAG_SLOT) == slot) {', 'if (compound.getIntOr(TAG_SLOT, 0) == slot) {'),
  ('int slot = listNBT.getCompound(i).getInt(TAG_SLOT);', 'int slot = listNBT.getCompoundOrEmpty(i).getIntOr(TAG_SLOT, 0);'),
])

# attachments serialize through value IO since 1.21.6, keep the old NBT inside the attachment's own object
edit(T + 'library/tools/capability/EntityModifierCapability.java', [('''      .serialize(new IAttachmentSerializer<ListTag,ModifierNBT>() {
        @Override
        public ModifierNBT read(IAttachmentHolder holder, ListTag tag, HolderLookup.Provider provider) {
          return ModifierNBT.readFromNBT(tag);
        }

        @Override
        public ListTag write(ModifierNBT attachment, HolderLookup.Provider provider) {
          return attachment.serializeToNBT();
        }
      })''', '''      .serialize(new IAttachmentSerializer<ModifierNBT>() {
        @Override
        public ModifierNBT read(IAttachmentHolder holder, ValueInput input) {
          return ModifierNBT.readFromNBT(ValueIOHelper.read(input).get("modifiers"));
        }

        @Override
        public boolean write(ModifierNBT attachment, ValueOutput output) {
          CompoundTag tag = new CompoundTag();
          tag.put("modifiers", attachment.serializeToNBT());
          ValueIOHelper.write(output, tag);
          return true;
        }
      })''')])
add_import(T + 'library/tools/capability/EntityModifierCapability.java', 'net.minecraft.nbt.CompoundTag', 'net.minecraft.world.level.storage.ValueInput', 'net.minecraft.world.level.storage.ValueOutput', 'slimeknights.mantle.util.ValueIOHelper')
edit(T + 'library/tools/capability/PersistentDataCapability.java', [('''      .serialize(new IAttachmentSerializer<CompoundTag,ModDataNBT>() {
        @Override
        public ModDataNBT read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
          return ModDataNBT.readFromNBT(tag);
        }

        @Override
        public CompoundTag write(ModDataNBT attachment, HolderLookup.Provider provider) {
          return attachment.getCopy();
        }
      })''', '''      .serialize(new IAttachmentSerializer<ModDataNBT>() {
        @Override
        public ModDataNBT read(IAttachmentHolder holder, ValueInput input) {
          return ModDataNBT.readFromNBT(ValueIOHelper.read(input));
        }

        @Override
        public boolean write(ModDataNBT attachment, ValueOutput output) {
          ValueIOHelper.write(output, attachment.getCopy());
          return true;
        }
      })''')])
add_import(T + 'library/tools/capability/PersistentDataCapability.java', 'net.minecraft.world.level.storage.ValueInput', 'net.minecraft.world.level.storage.ValueOutput', 'slimeknights.mantle.util.ValueIOHelper')

# item entity: invulnerable flag replaces the removed override, pickup delay is opened by the access transformer
edit(T + 'library/tools/IndestructibleItemEntity.java', [
  ('''    this.lifespan = Integer.MAX_VALUE;
  }''', '''    this.lifespan = Integer.MAX_VALUE;
    // prevent any damage besides out of world, damage methods are final since 1.21.2
    this.setInvulnerable(true);
  }'''),
  ('''      CompoundTag data = new CompoundTag();
      itemEntity.saveWithoutId(data);
      this.setPickUpDelay(data.getShortOr("PickupDelay", (short) 0));''', '''      this.setPickUpDelay(itemEntity.pickupDelay);'''),
  ('''  @Override
  public boolean isInvulnerableTo(DamageSource pSource) {
    // prevent any damage besides out of world
    return !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
  }

''', ''),
])

edit(T + 'library/tools/capability/inventory/ToolInventoryCapability.java', [('return InteractionResult.sidedSuccess(player.level().isClientSide());', 'return InteractionResult.SUCCESS;')])
edit(T + 'library/tools/definition/module/weapon/SweepWeaponAttack.java', [('(attacker.walkDist - attacker.walkDistO) < attacker.getSpeed()', 'attacker.getKnownMovement().horizontalDistanceSqr() < Mth.square(attacker.getSpeed() * 2.5)')])
add_import(T + 'library/tools/definition/module/weapon/SweepWeaponAttack.java', 'net.minecraft.util.Mth')

# volatile and persistent data shortcuts
edit(T + 'library/tools/helper/ModifierUtil.java', [
  ('nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).getBoolean(flag.toString())', 'nbt.getCompoundOrEmpty(ToolStack.TAG_VOLATILE_MOD_DATA).getBooleanOr(flag.toString(), false)'),
  ('nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).contains(key.toString())', 'nbt.getCompoundOrEmpty(ToolStack.TAG_VOLATILE_MOD_DATA).contains(key.toString())'),
  ('nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).getInt(flag.toString())', 'nbt.getCompoundOrEmpty(ToolStack.TAG_VOLATILE_MOD_DATA).getIntOr(flag.toString(), 0)'),
  ('nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA).getString(flag.toString())', 'nbt.getCompoundOrEmpty(ToolStack.TAG_PERSISTENT_MOD_DATA).getStringOr(flag.toString(), "")'),
])
# NeoForge removed the shield ability with vanilla's blocking component in 1.21.5, Tinkers keeps its own
edit(T + 'tools/TinkerItemAbilities.java', [('  public static final ItemAbility SHIELD_DISABLE = ItemAbility.get("shield_disable");', '  public static final ItemAbility SHIELD_DISABLE = ItemAbility.get("shield_disable");\n  /** Tool can block like a shield, NeoForge removed its own ability in 1.21.5 */\n  public static final ItemAbility SHIELD_BLOCK = ItemAbility.get("shield_block");')])
for p in ['library/tools/helper/ModifierUtil.java', 'library/tools/helper/TooltipUtil.java', 'library/tools/item/ranged/ModifiableCrossbowItem.java', 'tools/modifiers/ability/tool/ParryingModifier.java', 'tools/modules/armor/CounterModule.java', 'tools/modules/armor/KnockbackCounterModule.java', 'tools/modules/combat/BlockingModule.java']:
  text = read(T + p)
  if 'ItemAbilities.SHIELD_BLOCK' in text:
    text = text.replace('ItemAbilities.SHIELD_BLOCK', 'TinkerItemAbilities.SHIELD_BLOCK')
    write(T + p, text)
    add_import(T + p, 'slimeknights.tconstruct.tools.TinkerItemAbilities')

edit(T + 'library/tools/helper/ToolDamageUtil.java', [
  ('nbt != null && nbt.getBoolean(ToolStack.TAG_BROKEN)', 'nbt != null && nbt.getBooleanOr(ToolStack.TAG_BROKEN, false)'),
  ('LivingEntity.getSlotForHand(hand)', 'hand.asEquipmentSlot()'),
])
edit(T + 'library/tools/helper/TooltipUtil.java', [('nbt != null && nbt.getBoolean(KEY_DISPLAY)', 'nbt != null && nbt.getBooleanOr(KEY_DISPLAY, false)')])
edit(T + 'library/modifiers/modules/behavior/InfinityModule.java', [('tag != null && tag.getBoolean(INFINITY)', 'tag != null && tag.getBooleanOr(INFINITY, false)')])
edit(T + 'library/tools/helper/ToolHarvestLogic.java', [('state.onDestroyedByPlayer(world, pos, context.getPlayer(), context.canHarvest(), world.getFluidState(pos))', 'state.onDestroyedByPlayer(world, pos, context.getPlayer(), context.getPlayer().getMainHandItem(), context.canHarvest(), world.getFluidState(pos))')])
edit(T + 'library/tools/layout/LayoutIcon.java', [
  ('var item = BuiltInRegistries.ITEM.get(Identifier.parse(GsonHelper.getAsString(object, "item")));\n        if (item == null)', 'var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(GsonHelper.getAsString(object, "item"))).orElse(null);\n        if (item == null)'),
  ('TagParser.parseTag(', 'TagParser.parseCompoundFully('),
])
edit(T + 'library/tools/layout/StationSlotLayout.java', [('translationKey = stack.getDescriptionId();', 'translationKey = stack.getItem().getDescriptionId();')])

# CompoundTag getters return optionals since 1.21.5
GETTERS = [('getInt', '0'), ('getBoolean', 'false'), ('getFloat', '0f'), ('getString', '""')]
text = read(T + 'library/tools/nbt/IModDataView.java')
for name, default in GETTERS:
  text = text.replace(f'get(name, CompoundTag::{name});', f'get(name, (tag, key) -> tag.{name}Or(key, {default}));')
text = text.replace('get(name, CompoundTag::getCompound);', 'get(name, CompoundTag::getCompoundOrEmpty);')
write(T + 'library/tools/nbt/IModDataView.java', text)
text = read(T + 'library/utils/RestrictedCompoundTag.java')
for name, default in GETTERS:
  text = text.replace(f'get(name, CompoundTag::{name}, {default if name != "getFloat" else "0f"});', f'get(name, (tag, key) -> tag.{name}Or(key, {default}), {default});')
text = text.replace('&& tag.contains(name, type);', '&& TagUtil.contains(tag, name, type);')
write(T + 'library/utils/RestrictedCompoundTag.java', text)
add_import(T + 'library/utils/RestrictedCompoundTag.java', 'slimeknights.tconstruct.library.utils.TagUtil')
edit(T + 'library/tools/nbt/ModDataNBT.java', [('return data.contains(name.toString(), type);', 'return TagUtil.contains(data, name.toString(), type);')])
add_import(T + 'library/tools/nbt/ModDataNBT.java', 'slimeknights.tconstruct.library.utils.TagUtil')
edit(T + 'library/utils/TagUtil.java', [('public final class TagUtil {', '''public final class TagUtil {
  /** Type for {@link #contains(CompoundTag, String, int)} matching any number, vanilla removed its constant in 1.21.5 */
  public static final int TAG_ANY_NUMERIC = 99;

  /** Checks if the tag has the key with the given type, replacement for the removed {@code CompoundTag#contains(String, int)} */
  public static boolean contains(CompoundTag tag, String key, int type) {
    Tag value = tag.get(key);
    return value != null && (value.getId() == type || type == TAG_ANY_NUMERIC && value instanceof NumericTag);
  }
''')], required=False)

# list tags no longer have a single element type
for p, t in [('library/tools/nbt/MaterialIdNBT.java', 'TAG_STRING'), ('library/tools/nbt/ModifierNBT.java', 'TAG_COMPOUND')]:
  edit(T + p, [(f'if (listNBT.getElementType() != Tag.{t}) {{', f'if (listNBT.isEmpty() || listNBT.get(0).getId() != Tag.{t}) {{')])
edit(T + 'library/tools/nbt/MaterialNBT.java', [('if (listNBT.getElementType() != Tag.TAG_STRING || listNBT.isEmpty()) {', 'if (listNBT.isEmpty() || listNBT.get(0).getId() != Tag.TAG_STRING) {')])
edit(T + 'library/tools/nbt/MaterialIdNBT.java', [('.map(Tag::getAsString)', '.map(tag -> tag.asString().orElse(""))')])
edit(T + 'library/tools/stat/FloatToolStat.java', [('((NumericTag) tag).getAsFloat()', '((NumericTag) tag).floatValue()')])
edit(T + 'library/tools/stat/MaxToolStat.java', [('((NumericTag)tag).getAsInt()', '((NumericTag)tag).intValue()')])
edit(T + 'library/utils/IdParser.java', [('Identifier.isAllowedInResourceLocation(', 'Identifier.isAllowedInIdentifier(')])
edit(T + 'library/utils/ItemStackUtil.java', [('return tag != null && tag.getCompound(key).isPresent() ? tag.getCompound(key) : null;', 'return tag != null ? tag.getCompound(key).orElse(null) : null;')])
edit(T + 'library/utils/PotionUtils.java', [
  ('return PotionContents.getColor(potion.getEffects());', 'return getColor(potion.getEffects());'),
  ('return PotionContents.getColor(effects);', 'return PotionContents.getColorOptional(effects).orElse(PotionContents.BASE_POTION_COLOR);'),
  ('getPotionContents(stack).addPotionTooltip(tooltip::add, durationFactor, 20.0f);', 'PotionContents.addPotionTooltip(getPotionContents(stack).getAllEffects(), tooltip::add, durationFactor, 20.0f);'),
])
# registries are their own lookups since 1.21.2
edit(T + 'library/utils/RegistryAccessUtil.java', [('BuiltInRegistries.REGISTRY.stream().map(Registry::asLookup));', 'BuiltInRegistries.REGISTRY.stream().map(registry -> (HolderLookup.RegistryLookup<?>) registry));')])
edit(T + 'library/utils/SlimeBounceHandler.java', [('entity.hasImpulse = true;', 'entity.needsSync = true;')])
text = read(T + 'library/utils/TeleportHelper.java')
text = text.replace('import net.minecraft.world.entity.RelativeMovement;', 'import net.minecraft.world.entity.Relative;').replace('RelativeMovement', 'Relative')
write(T + 'library/utils/TeleportHelper.java', text)

# per stack names: description ids are final, creator mod takes registries
edit(T + 'library/tools/part/MaterialItem.java', [('String key = self.asItem().getDescriptionId(stack);', 'String key = self.asItem().getDescriptionId();')])
for p in ['library/tools/part/MaterialItem.java', 'library/tools/part/block/MaterialBlockItem.java', 'library/tools/part/block/ToolPartBlockItem.java']:
  text = read(T + p)
  if 'public String getCreatorModId(ItemStack stack)' in text:
    text = text.replace('public String getCreatorModId(ItemStack stack)', 'public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack)')
    write(T + p, text)
    add_import(T + p, 'net.minecraft.core.HolderLookup')
print('ok')
