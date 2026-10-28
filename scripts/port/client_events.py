"""Ports the Tinkers client event classes to 1.21.11.

Model, item color and armor registrations are removed here, they move to the model and armor ports
(listed in PORTING_1.21.11.md) as item models, tint sources and equipment assets replace them.
"""
from javaport import port

T = 'src/main/java/slimeknights/tconstruct/'


RELOAD = ('static void addResourceListener(RegisterClientReloadListenersEvent manager) {', 'static void addResourceListener(AddClientReloadListenersEvent manager) {')

port(T + 'tools/ToolClientEvents.java',
     methods=['static void registerSpriteSourceTypes(', 'static void registerModelLoaders(', 'static void registerModifierModels(',
              'static void replaceCapeLayers(', 'private static class TinkerCapeLayer', 'static void itemColors('],
     lines=['ModifierModelManager.init(manager);', 'manager.registerReloadListener(ModifierModelMapManager.INSTANCE);', 'DynamicTextureLoader.init(manager);',
            'manager.registerReloadListener(SlimeskullArmorModel.RELOAD_LISTENER);', 'ArmorModelManager.init(manager);',
            'manager.registerReloadListener(TrimArmorTextureSupplier.CACHE_INVALIDATOR);', 'AbstractArmorModel.init();', 'TinkerItemProperties.',
            'Consumer<Item> brokenConsumer = TinkerItemProperties::registerBrokenProperty;', 'TinkerTools.travelersGear.forEach(brokenConsumer);',
            'TinkerTools.plateArmor.forEach(brokenConsumer);', 'TinkerTools.slimesuit.forEach(brokenConsumer);',
            'event.registerEntityRenderer(TinkerTools.fishingHook.get(), CombatFishingHookRenderer::new);'],
     pairs=[RELOAD,
            ('    manager.registerReloadListener(MODIFIER_RELOAD_LISTENER);', '    manager.addListener(getResource("modifier_cache"), MODIFIER_RELOAD_LISTENER);'),
            ('    manager.registerReloadListener(HarvestTiers.RELOAD_LISTENER);', '    manager.addListener(getResource("harvest_tiers"), HarvestTiers.RELOAD_LISTENER);'),
            ('''      if (ModList.get().isLoaded("twilightforest")) {
      }
''', ''),
            ('''      // properties
      // stone
''', '')],
     imports=['net.neoforged.neoforge.client.event.AddClientReloadListenersEvent'])
print("ok")
