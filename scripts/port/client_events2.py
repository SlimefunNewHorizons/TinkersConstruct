"""Ports the remaining Tinkers client event classes to 1.21.11, see client_events.py"""
from javaport import port

T = 'src/main/java/slimeknights/tconstruct/'

# world: skull models take their texture on registration, slimeskull heads move to the armor port
port(T + 'world/WorldClientEvents.java',
     methods=['static void clientSetup(FMLClientSetupEvent event)', 'static void registerItemColorHandlers(', 'private static void registerHeadModel(', 'private static void registerPiglinHeadModel('],
     pairs=[('''  static void addResourceListener(RegisterClientReloadListenersEvent event) {
    for (FoliageType type : FoliageType.values()) {
      event.registerReloadListener(new SlimeColorReloadListener(type));
    }''', '''  static void addResourceListener(AddClientReloadListenersEvent event) {
    for (FoliageType type : FoliageType.values()) {
      event.addListener(TConstruct.getResource("slime_colors/" + type.getSerializedName()), new SlimeColorReloadListener(type));
    }'''),
            ('''  static void registerSkullModels(EntityRenderersEvent.CreateSkullModels event) {
    EntityModelSet modelSet = event.getEntityModelSet();
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> {
      if (type.isPiglin()) {
        event.registerSkullModel(type, new PiglinHeadModel(modelSet.bakeLayer(layer)));
      } else {
        event.registerSkullModel(type, new SkullModel(modelSet.bakeLayer(layer)));
      }
    });''', '''  static void registerSkullModels(EntityRenderersEvent.CreateSkullModels event) {
    // shortcut: slimeskull helmet heads are not registered, they move to equipment assets with the armor port
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> event.registerSkullModel(type, layer, type.isPiglin() ? PiglinHeadModel::new : SkullModel::new, SKULL_TEXTURES.get(type)));'''),
            ('''public class WorldClientEvents extends ClientEventBase {''', '''public class WorldClientEvents extends ClientEventBase {
  /** Textures for each Tinkers skull */
  private static final Map<TinkerHeadType,Identifier> SKULL_TEXTURES = new EnumMap<>(Map.ofEntries(
    Map.entry(TinkerHeadType.BLAZE, Identifier.parse("textures/entity/blaze.png")),
    Map.entry(TinkerHeadType.ENDERMAN, TConstruct.getResource("textures/entity/skull/enderman.png")),
    Map.entry(TinkerHeadType.STRAY, TConstruct.getResource("textures/entity/skull/stray.png")),
    Map.entry(TinkerHeadType.HUSK, Identifier.parse("textures/entity/zombie/husk.png")),
    Map.entry(TinkerHeadType.DROWNED, TConstruct.getResource("textures/entity/skull/drowned.png")),
    Map.entry(TinkerHeadType.SPIDER, Identifier.parse("textures/entity/spider/spider.png")),
    Map.entry(TinkerHeadType.CAVE_SPIDER, Identifier.parse("textures/entity/spider/cave_spider.png")),
    Map.entry(TinkerHeadType.PIGLIN_BRUTE, Identifier.parse("textures/entity/piglin/piglin_brute.png")),
    Map.entry(TinkerHeadType.ZOMBIFIED_PIGLIN, Identifier.parse("textures/entity/piglin/zombified_piglin.png")),
    Map.entry(TinkerHeadType.VENOMBONE, TConstruct.getResource("textures/entity/skull/venombone.png")),
    Map.entry(TinkerHeadType.BLAZING_BONE, TConstruct.getResource("textures/entity/skull/blazing_bone.png")),
    Map.entry(TinkerHeadType.NECRONIUM, TConstruct.getResource("textures/entity/skull/necronium.png"))));
''')],
     imports=['net.neoforged.neoforge.client.event.AddClientReloadListenersEvent', 'java.util.EnumMap', 'java.util.Map'])

# smeltery: model loaders move to the model port, tank renderers need the context
port(T + 'smeltery/SmelteryClientEvents.java',
     methods=['static void registerModelLoaders('],
     lines=['ToolModel.registerSmallTool('],
     pairs=[('static void addResourceListener(RegisterClientReloadListenersEvent event) {', 'static void addResourceListener(AddClientReloadListenersEvent event) {'),
            ('context -> new TankInventoryBlockEntityRenderer<>(BlockStateProperties.FACING)', 'context -> new TankInventoryBlockEntityRenderer<>(context, BlockStateProperties.FACING)'),
            ('context -> new TankInventoryBlockEntityRenderer<>(BlockStateProperties.HORIZONTAL_FACING)', 'context -> new TankInventoryBlockEntityRenderer<>(context, BlockStateProperties.HORIZONTAL_FACING)')],
     imports=['net.neoforged.neoforge.client.event.AddClientReloadListenersEvent'])

# tables: chest tint moves to the item definition
port(T + 'tables/TableClientEvents.java',
     methods=['static void registerItemColors('],
     pairs=[('BlockEntityRendererProvider<TableBlockEntity> tableRenderer = InventoryBlockEntityRenderer::new;', 'BlockEntityRendererProvider<TableBlockEntity,InventoryBlockEntityRenderer.State> tableRenderer = InventoryBlockEntityRenderer::new;')])

# fluids: potion tint moves to the item definition, render layers are set in the fluid models
port(T + 'fluids/FluidClientEvents.java',
     methods=['static void itemColors(', 'static void registerModelLoaders('])

# gadgets: frame models are standalone models
port(T + 'gadgets/GadgetClientEvents.java',
     pairs=[('''  static void registerModels(RegisterAdditional event) {
    FancyItemFrameRenderer.LOCATIONS_MODEL.values().forEach(event::register);
    FancyItemFrameRenderer.LOCATIONS_MODEL_MAP.values().forEach(event::register);''', '''  static void registerModels(ModelEvent.RegisterStandalone event) {
    FancyItemFrameRenderer.registerModels(event);''')],
     imports=['net.neoforged.neoforge.client.event.ModelEvent'])

# commons: GUI model loader moves to the model port
port(T + 'shared/CommonsClientEvents.java',
     methods=['static void registerModelLoaders('],
     pairs=[('static void addResourceListeners(RegisterClientReloadListenersEvent event) {', 'static void addResourceListeners(AddClientReloadListenersEvent event) {')],
     imports=['net.neoforged.neoforge.client.event.AddClientReloadListenersEvent'])
print('ok 2')
