"""Tinkers 1.21.11 fixes, batch 3: world, entities, blocks, tool modules, recipes."""
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


def replace_all(path, pairs):
  text = read(path)
  for old, new in pairs:
    text = text.replace(old, new)
  write(path, text)


SUCCESS_SWING = '(result instanceof InteractionResult.Success success && success.swingSource() != InteractionResult.SwingSource.NONE)'

# world: heads, creepers, dirt tiers, islands, vegetation
edit(T + 'world/TinkerWorld.java', [
  ('this.setSuccess(ArmorItem.dispenseArmor(source, stack));', 'this.setSuccess(EquipmentDispenseItemBehavior.dispenseEquipment(source, stack));'),
  ('import net.minecraft.world.item.ArmorItem;\n', 'import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;\n'),
  ('BlockBehaviour.Properties.of().strength(1.0F).lootFrom(() -> heads.get(type));', 'BlockBehaviour.Properties.of().strength(1.0F).overrideLootTable(heads.get(type).getLootTable()).overrideDescription(heads.get(type).getDescriptionId());'),
  ('new EndermanHeadItem(heads.get(type), wallHeads.get(type), properties, Direction.DOWN)', 'new EndermanHeadItem(heads.get(type), wallHeads.get(type), Direction.DOWN, properties)'),
  ('new StandingAndWallBlockItem(heads.get(type), wallHeads.get(type), properties, Direction.DOWN)', 'new StandingAndWallBlockItem(heads.get(type), wallHeads.get(type), Direction.DOWN, properties)'),
])
edit(T + 'world/item/EndermanHeadItem.java', [
  ('public EndermanHeadItem(Block pBlock, Block pWallBlock, Properties pProperties, Direction pAttachmentDirection) {\n    super(pBlock, pWallBlock, pProperties, pAttachmentDirection);', 'public EndermanHeadItem(Block pBlock, Block pWallBlock, Direction pAttachmentDirection, Properties pProperties) {\n    super(pBlock, pWallBlock, pAttachmentDirection, pProperties);'),
  ('public boolean isEnderMask(ItemStack stack, Player player, EnderMan endermanEntity) {', 'public boolean isGazeDisguise(ItemStack stack, Player player, @Nullable LivingEntity entity) {'),
  ('import net.minecraft.world.entity.monster.EnderMan;\n', 'import net.minecraft.world.entity.LivingEntity;\n\nimport javax.annotation.Nullable;\n'),
])
edit(T + 'world/WorldEvents.java', [
  ('if (creeper.canDropMobsSkull()) {', 'if (creeper.isPowered() && !creeper.droppedSkulls && dying(event).level() instanceof ServerLevel server) {'),
  ('            creeper.increaseDroppedSkulls();\n            event.getDrops().add(dying.spawnAtLocation(TinkerWorld.heads.get(headType)));', '            creeper.droppedSkulls = true;\n            event.getDrops().add(dying.spawnAtLocation(server, TinkerWorld.heads.get(headType)));'),
])
# the dying entity is declared inside the block, use the event directly in the condition
replace_all(T + 'world/WorldEvents.java', [('dying(event).level() instanceof ServerLevel server', 'event.getEntity().level() instanceof ServerLevel server')])
add_import(T + 'world/WorldEvents.java', 'net.minecraft.server.level.ServerLevel')
for p in ['world/block/DirtType.java', 'world/block/FoliageType.java']:
  replace_all(T + p, [('Tiers harvestTier', 'ToolMaterial harvestTier'), ('Tiers.', 'ToolMaterial.'), ('import net.minecraft.world.item.Tiers;', 'import net.minecraft.world.item.ToolMaterial;')])
LIGHT = [('aboveState.getLightBlock(world, above)) < world.getMaxLightLevel()', 'aboveState.getLightBlock()) < LightEngine.MAX_LEVEL'),
         ('LightEngine.getLightBlockInto(world, targetState, pos, aboveState, above, Direction.UP,', 'LightEngine.getLightBlockInto(targetState, aboveState, Direction.UP,'),
         ('LightEngine.getLightBlockInto(reader, state, pos, blockstate, blockpos, Direction.UP, blockstate.getLightBlock(reader, blockpos));', 'LightEngine.getLightBlockInto(state, blockstate, Direction.UP, blockstate.getLightBlock());'),
         ('return i < reader.getMaxLightLevel();', 'return i < LightEngine.MAX_LEVEL;')]
replace_all(T + 'world/block/SlimeGrassBlock.java', LIGHT)
replace_all(T + 'world/block/SlimeNyliumBlock.java', LIGHT)
edit(T + 'world/block/SlimeLeavesBlock.java', [
  ('public class SlimeLeavesBlock extends LeavesBlock {', 'public class SlimeLeavesBlock extends TintedParticleLeavesBlock {'),
  ('    super(properties);\n    this.foliageType = foliageType;', '    super(0.01f, properties);\n    this.foliageType = foliageType;'),
])
add_import(T + 'world/block/SlimeLeavesBlock.java', 'net.minecraft.world.level.block.TintedParticleLeavesBlock')
edit(T + 'world/block/SlimePropaguleBlock.java', [('Vec3 vec3 = pState.getOffset(pLevel, pPos);', 'Vec3 vec3 = pState.getOffset(pPos);')])
# old BushBlock became VegetationBlock in 1.21.5, BushBlock is now the bush plant
for p in ['world/block/SlimeTallGrassBlock.java']:
  replace_all(T + p, [('extends BushBlock', 'extends VegetationBlock'), ('import net.minecraft.world.level.block.BushBlock;', 'import net.minecraft.world.level.block.VegetationBlock;')])
edit(T + 'world/worldgen/islands/IslandPiece.java', [('context.registryAccess().registry(registryKey)', 'context.registryAccess().lookup(registryKey)'), ('.getRandomValue(rand)', '.getRandom(rand)')])
replace_all(T + 'world/worldgen/islands/IslandStructure.java', [
  ('SimpleWeightedRandomList.wrappedCodec(', 'WeightedList.codec('), ('SimpleWeightedRandomList', 'WeightedList'), ('.getRandomValue(random)', '.getRandom(random)'),
])
edit(T + 'world/worldgen/trees/ExtraRootVariantPlacer.java', [('BuiltInRegistries.BLOCK.getOrCreateTag(tag)', 'IngredientHelper.tagHolder(BuiltInRegistries.BLOCK, tag)')])
add_import(T + 'world/worldgen/trees/ExtraRootVariantPlacer.java', 'slimeknights.mantle.recipe.helper.IngredientHelper')
edit(T + 'world/logic/AncientToolItemListing.java', [('public MerchantOffer getOffer(Entity trader, RandomSource random) {', 'public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {')])
add_import(T + 'world/logic/AncientToolItemListing.java', 'net.minecraft.server.level.ServerLevel')

# entities: hurt and fall methods take the server level, drop chances are a record
edit(T + 'world/entity/ArmoredSlimeEntity.java', [
  ('this.armorDropChances[EquipmentSlot.HEAD.getIndex()] = 0.0F;', 'this.setDropChance(EquipmentSlot.HEAD, 0.0F);'),
  ('this.getEquipmentDropChance(EquipmentSlot.HEAD)', 'this.getDropChances().byEquipment(EquipmentSlot.HEAD)'),
  ('float dropChance = getEquipmentDropChance(EquipmentSlot.HEAD);', 'float dropChance = getDropChances().byEquipment(EquipmentSlot.HEAD);'),
  ('        this.spawnAtLocation(stack);', '        this.spawnAtLocation(level, stack);'),
  ('ArmoredSlimeEntity slime = this.getType().create(level);', 'ArmoredSlimeEntity slime = this.getType().create(level, EntitySpawnReason.TRIGGERED);'),
  ('slime.moveTo(', 'slime.snapTo('),
])
add_import(T + 'world/entity/ArmoredSlimeEntity.java', 'net.minecraft.world.entity.EntitySpawnReason')
edit(T + 'world/entity/EnderSlimeEntity.java', [
  ('public boolean doHurtTarget(Entity target) {\n    boolean hurt = super.doHurtTarget(target);', 'public boolean doHurtTarget(ServerLevel level, Entity target) {\n    boolean hurt = super.doHurtTarget(level, target);'),
  ('protected void actuallyHurt(DamageSource damageSrc, float damageAmount) {\n    float oldHealth = getHealth();\n    super.actuallyHurt(damageSrc, damageAmount);', 'protected void actuallyHurt(ServerLevel level, DamageSource damageSrc, float damageAmount) {\n    float oldHealth = getHealth();\n    super.actuallyHurt(level, damageSrc, damageAmount);'),
])
add_import(T + 'world/entity/EnderSlimeEntity.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'world/entity/SkySlimeEntity.java', [
  ('public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {', 'public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource source) {'),
  ('''    float[] ret = CommonHooks.onLivingFall(this, distance, damageMultiplier);
    if (ret == null) {
      return false;
    }
    distance = ret[0];''', '''    LivingFallEvent fall = CommonHooks.onLivingFall(this, distance, damageMultiplier);
    if (fall.isCanceled()) {
      return false;
    }
    distance = fall.getDistance();'''),
  ('      hasImpulse = true;', '      needsSync = true;'),
])
add_import(T + 'world/entity/SkySlimeEntity.java', 'net.neoforged.neoforge.event.entity.living.LivingFallEvent')
edit(T + 'world/entity/TerracubeEntity.java', [('protected int calculateFallDamage(float distance, float damageMultiplier) {', 'protected int calculateFallDamage(double distance, float damageMultiplier) {')])

# blocks: fall handling uses doubles and new movement hook names
for p in ['world/block/CongealedSlimeBlock.java', 'shared/block/SlimesteelBlock.java']:
  text = read(T + p)
  text = re.sub(r'public void fallOn\(Level (\w+), BlockState (\w+), BlockPos (\w+), Entity (\w+), float (\w+)\)', r'public void fallOn(Level \1, BlockState \2, BlockPos \3, Entity \4, double \5)', text)
  text = text.replace('updateEntityAfterFallOn(', 'updateEntityMovementAfterFallOn(')
  write(T + p, text)
replace_all(T + 'shared/effect/AntigravityEffect.java', [('hit.getBlock().updateEntityAfterFallOn(level, living);', 'hit.getBlock().updateEntityMovementAfterFallOn(level, living);'), ('isInWaterOrBubble()', 'isInWater()')])
replace_all(T + 'tools/logic/DoubleJumpHandler.java', [('isInWaterOrBubble()', 'isInWater()')])

# tool logic
edit(T + 'tools/TinkerTools.java', [('''        if (entity instanceof Player player && player.isBlocking()) {
          player.disableShield();
        }''', '''        // shields are disabled through their blocking component since 1.21.5, vanilla's axe disables for 5 seconds
        ItemStack blocking = entity.getItemBlockingWith();
        BlocksAttacks blocks = blocking == null ? null : blocking.get(DataComponents.BLOCKS_ATTACKS);
        if (entity instanceof Player && blocks != null && entity.level() instanceof ServerLevel server) {
          blocks.disable(server, entity, 5, blocking);
        }''')])
add_import(T + 'tools/TinkerTools.java', 'net.minecraft.world.item.component.BlocksAttacks', 'net.minecraft.core.component.DataComponents', 'net.minecraft.server.level.ServerLevel', 'net.minecraft.world.item.ItemStack')
replace_all(T + 'tools/logic/InteractionHandler.java', [('if (result.shouldSwing()) {', 'if ' + SUCCESS_SWING + ' {'), ('      } else {\n        event.setCanceled(true);\n      }', '      } else {\n        event.setBlocked(false);\n      }')])
replace_all(T + 'tools/network/InteractWithAirPacket.java', [('if (result.shouldSwing()) {', 'if ' + SUCCESS_SWING + ' {')])
edit(T + 'tools/logic/ModifierEvents.java', [
  ('living.hasImpulse = true;', 'living.needsSync = true;'),
  ('SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS', 'SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS'),
  ('''          if (!level.isClientSide() && arrow.shotFromCrossbow() && owner instanceof ServerPlayer player) {
            if (arrow.piercedAndKilledEntities != null) {
              CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(player, arrow.piercedAndKilledEntities);
            } else if (!target.isAlive()) {
              CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(player, List.of(target));
            }
          }''', '''          if (!level.isClientSide() && owner instanceof ServerPlayer player) {
            if (arrow.piercedAndKilledEntities != null) {
              CriteriaTriggers.KILLED_BY_ARROW.trigger(player, arrow.piercedAndKilledEntities, arrow.getWeaponItem());
            } else if (!target.isAlive()) {
              CriteriaTriggers.KILLED_BY_ARROW.trigger(player, List.of(target), arrow.getWeaponItem());
            }
          }'''),
  ('arrow.spawnAtLocation(arrow.getPickupItemStackOrigin().copy(), 0.1F);', 'arrow.spawnAtLocation((ServerLevel) level, arrow.getPickupItemStackOrigin().copy(), 0.1F);'),
])
add_import(T + 'tools/logic/ModifierEvents.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'tools/modules/armor/SleevesModule.java', [('''            inventory.setStack(tool, modifier, selected, result.getObject());
            return result.getResult();''', '''            ItemStack transformed = result instanceof InteractionResult.Success success ? success.heldItemTransformedTo() : null;
            inventory.setStack(tool, modifier, selected, transformed != null ? transformed : ammo);
            return result;''')])
edit(T + 'tools/modules/armor/ToolBeltModule.java', [('inventory.items.size()', 'inventory.getNonEquipmentItems().size()')])
edit(T + 'tools/modules/armor/MinimapModule.java', [('map.inventoryTick(world, holder, Inventory.SLOT_OFFHAND, true);', 'map.inventoryTick(world, holder, EquipmentSlot.OFFHAND);')])
add_import(T + 'tools/modules/armor/MinimapModule.java', 'net.minecraft.world.entity.EquipmentSlot')
edit(T + 'tools/modules/combat/ChannelingModule.java', [('EntityType.LIGHTNING_BOLT.create(level);', 'EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);'), ('lightning.moveTo(', 'lightning.snapTo(')])
add_import(T + 'tools/modules/combat/ChannelingModule.java', 'net.minecraft.world.entity.EntitySpawnReason')
edit(T + 'tools/modules/cosmetic/BannerModule.java', [('patterns.getCompound(0).getInt(KEY_DYE)', 'patterns.getCompoundOrEmpty(0).getIntOr(KEY_DYE, 0)')])
edit(T + 'tools/modules/cosmetic/TrimModule.java', [('access.lookupOrThrow(Registries.TRIM_PATTERN).get(Identifier.tryParse(trimPattern))', 'access.lookupOrThrow(Registries.TRIM_PATTERN).getValue(Identifier.tryParse(trimPattern))')])
edit(T + 'tools/modules/interaction/BrushModule.java', [('brushable.brush(level.getGameTime(), player, blockHit.getDirection())', 'level instanceof ServerLevel server && brushable.brush(level.getGameTime(), server, player, blockHit.getDirection(), player.getMainHandItem())')])
add_import(T + 'tools/modules/interaction/BrushModule.java', 'net.minecraft.server.level.ServerLevel')
for p in ['tools/modules/interaction/FireballModule.java', 'tools/modules/interaction/PlaceGlowModule.java']:
  text = read(T + p)
  write(T + p, re.sub(r'InteractionResult\.sidedSuccess\([^;]*\);', 'InteractionResult.SUCCESS;', text))
# cooldowns are keyed by stack since 1.21.2, a plain stack of the item shares the item's cooldown group
for p in ['tools/modifiers/ability/tool/ParryingModifier.java', 'tools/modules/interaction/SplashingModule.java', 'tools/modules/interaction/sling/SlingKnockbackModule.java', 'tools/modules/interaction/sling/SlingLeapModule.java', 'tools/modules/interaction/sling/SlingTeleportModule.java']:
  replace_all(T + p, [('player.getCooldowns().addCooldown(tool, ', 'player.getCooldowns().addCooldown(new ItemStack(tool.getItem()), ')])
  add_import(T + p, 'net.minecraft.world.item.ItemStack')
edit(T + 'tools/modules/interaction/sling/SlingKnockbackModule.java', [('ProjectileUtil.getEntityHitResult(level, player, start, direction, bb, e -> e instanceof LivingEntity)', 'ProjectileUtil.getEntityHitResult(level, player, start, direction, bb, e -> e instanceof LivingEntity, 0.3F)')])
edit(T + 'tools/modules/SmeltingModule.java', [('recipe.value().getCookingTime()', 'recipe.value().cookingTime()'), ('cookingRecipe.getCookingTime()', 'cookingRecipe.cookingTime()'), ('cookingRecipe.getExperience()', 'cookingRecipe.experience()')])
edit(T + 'tools/modules/MeltingModule.java', [('context.getParam(LootContextParams.BLOCK_STATE)', 'context.getParameter(LootContextParams.BLOCK_STATE)')])
edit(T + 'tools/modifiers/loot/ModifierBonusLootFunction.java', [('context.getParam(LootContextParams.TOOL)', 'context.getParameter(LootContextParams.TOOL)')])
edit(T + 'tools/modifiers/slotless/StatOverrideModifier.java', [('((NumericTag)tag).getAsFloat()', '((NumericTag)tag).floatValue()')])
# biome temperature is private since 1.21.2, opened by the access transformer and needs the sea level
edit(T + 'tools/modifiers/traits/harvest/TemperateModifier.java', [('living.level().getBiome(pos).value().getTemperature(pos)', 'living.level().getBiome(pos).value().getTemperature(pos, living.level().getSeaLevel())')])
edit(T + 'tools/modifiers/traits/melee/InvariantModifier.java', [('living.level().getBiome(pos).value().getTemperature(pos)', 'living.level().getBiome(pos).value().getTemperature(pos, living.level().getSeaLevel())')])
edit(T + 'tools/modifiers/traits/ranged/OlympicModifier.java', [('targetEntity.spawnAtLocation(nugget);', 'targetEntity.spawnAtLocation((ServerLevel) targetEntity.level(), nugget);')])
add_import(T + 'tools/modifiers/traits/ranged/OlympicModifier.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'tools/modifiers/traits/skull/ChrysophiliteModifier.java', [('event.getDrops().add(target.spawnAtLocation(stack));', 'event.getDrops().add(target.spawnAtLocation((ServerLevel) target.level(), stack));')])
add_import(T + 'tools/modifiers/traits/skull/ChrysophiliteModifier.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'tools/modifiers/effect/BleedingEffect.java', [('if (target.level() instanceof ServerLevel serverLevel) {', 'if (target.level() instanceof ServerLevel targetLevel) {')])
replace_all(T + 'tools/modifiers/effect/BleedingEffect.java', [])
edit(T + 'tools/modules/ranged/ammo/TippedModule.java', [
  ('for (MobEffectInstance instance : BuiltInRegistries.POTION.get(id).getEffects()) {', 'for (MobEffectInstance instance : BuiltInRegistries.POTION.getValue(id).getEffects()) {'),
  ('if (effect.isInstantenous()) {\n            effect.applyInstantenousEffect(projectile, projectile.getOwner(), target, instance.getAmplifier(), 1f / (divisor * 0.75f));', 'if (effect.isInstantenous()) {\n            if (target.level() instanceof ServerLevel server) {\n              effect.applyInstantenousEffect(server, projectile, projectile.getOwner(), target, instance.getAmplifier(), 1f / (divisor * 0.75f));\n            }'),
  ('Component.translatable(Potion.getName(Optional.of(potion), "item.minecraft.potion.effect."))', 'Component.translatable(PotionUtils.getName(new PotionContents(potion), "item.minecraft.potion.effect."))'),
])
replace_all(T + 'tools/modules/ranged/ammo/TippedModule.java', [('BuiltInRegistries.POTION.getHolder(id).orElse(null)', 'BuiltInRegistries.POTION.get(id).orElse(null)')])
add_import(T + 'tools/modules/ranged/ammo/TippedModule.java', 'net.minecraft.server.level.ServerLevel', 'net.minecraft.world.item.alchemy.PotionContents')
edit(T + 'tools/menu/ToolContainerMenu.java', [('CraftingMenu.slotChangedCraftingGrid(this, player.level(), player, craftingContainer, resultContainer, null);', 'if (player.level() instanceof ServerLevel server) {\n      CraftingMenu.slotChangedCraftingGrid(this, server, player, craftingContainer, resultContainer, null);\n    }')])
add_import(T + 'tools/menu/ToolContainerMenu.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'tools/logic/ToolEvents.java', [
  ('if (entity.isInvulnerableTo(source)) {', 'if (entity.level() instanceof ServerLevel server && entity.isInvulnerableTo(server, source)) {'),
  ('armorStack.getItem() instanceof ArmorItem', 'armorStack.has(DataComponents.EQUIPPABLE)'),
  ('import net.minecraft.world.item.ArmorItem;\n', ''),
])
add_import(T + 'tools/logic/ToolEvents.java', 'net.minecraft.server.level.ServerLevel', 'net.minecraft.core.component.DataComponents')
edit(T + 'tools/entity/CombatFishingHook.java', [('this.moveTo(', 'this.snapTo(')])
replace_all(T + 'tools/entity/ThrownShuriken.java', [('this.spawnAtLocation(stack.copy());', 'this.spawnAtLocation((ServerLevel) this.level(), stack.copy());')])
add_import(T + 'tools/entity/ThrownShuriken.java', 'net.minecraft.server.level.ServerLevel')
edit(T + 'tools/entity/ThrownTool.java', [('tool.isBroken() ? SoundEvents.ITEM_BREAK : SoundEvents.TRIDENT_HIT', 'tool.isBroken() ? SoundEvents.ITEM_BREAK.value() : SoundEvents.TRIDENT_HIT')])
for p in ['tools/entity/FluidEffectProjectile.java', 'tools/entity/ModifiableArrow.java', 'tools/entity/ThrownShuriken.java', 'tools/entity/ThrownTool.java']:
  text = read(T + p)
  text = re.sub(r'(\w+)\.getFloat\(KEY_WATER_INERTIA\)', r'\1.getFloatOr(KEY_WATER_INERTIA, 0.6f)', text)
  text = re.sub(r'(\w+)\.getCompound\((KEY_STACK|KEY_FLUID)\)\)', r'\1.getCompoundOrEmpty(\2))', text)
  write(T + p, text)
edit(T + 'tools/item/CreativeSlotItem.java', [
  ('player.hasPermissions(MantleCommand.PERMISSION_GAME_COMMANDS)', 'MantleCommand.PERMISSION_GAME_COMMANDS.check(player.permissions())'),
  ('int updated = slots.getInt(name) + amount;', 'int updated = slots.getIntOr(name, 0) + amount;'),
])
edit(T + 'tools/item/ModifierCrystalItem.java', [('player.hasPermissions(MantleCommand.', 'MantleCommand.')], required=False)

# recipes
edit(T + 'tools/recipe/EnchantmentConvertingRecipe.java', [
  ('!tool.getItem().isEnchantable(tool)', '!tool.isEnchantable()'),
  ('EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level))', 'EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level))'),
  ('import net.minecraft.world.item.EnchantedBookItem;\n', ''),
])
add_import(T + 'tools/recipe/EnchantmentConvertingRecipe.java', 'net.minecraft.world.item.enchantment.EnchantmentHelper')
replace_all(T + 'tools/recipe/severing/MooshroomDemushroomingRecipe.java', [('import net.minecraft.world.entity.animal.cow.MushroomCow.MushroomType;', 'import net.minecraft.world.entity.animal.cow.MushroomCow;'), ('MushroomType.BROWN', 'MushroomCow.Variant.BROWN')])
edit(T + 'tools/recipe/severing/PlayerBeheadingRecipe.java', [('new ResolvableProfile(gameprofile)', 'ResolvableProfile.createResolved(gameprofile)')])
print('ok')
