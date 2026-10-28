"""Ports the Tinkers modifier models to 1.21.11, they only build quads so the logic stays"""
from javaport import port

M = 'src/main/java/slimeknights/mantle/client/model/util/'
T = 'src/main/java/slimeknights/tconstruct/library/client/'

# Mantle: quads are immutable records, copy them with a new color and light
port(M + 'ColoredBlockModel.java',
     pairs=[('''  /** Copies the quad with a static ARGB color, replacing its vertex colors */''', '''  /** Copies the quad with a static ARGB color and light emission, replacing its vertex colors */
  public static BakedQuad withColorAndLight(BakedQuad quad, int color, int lightEmission) {
    return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                         quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                         quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), Math.max(quad.lightEmission(), lightEmission),
                         quad.bakedNormals(), color == -1 ? quad.bakedColors() : BakedColors.of(color), quad.hasAmbientOcclusion());
  }

  /** Copies the quad with a static ARGB color, replacing its vertex colors */''')])

port(T + 'modifiers/model/FluidModifierModel.java',
     pairs=[('''    List<BlockElement> unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(-1, spriteGetter.apply(template)); // Use template as mask
    // TODO: is there anything that can be done about the fluid? to prevent weird offsets?
    List<BakedQuad> fluidQuads = UnbakedGeometryHelper.bakeElements(unbaked, mat -> fluidSprite, new SimpleModelState(transforms.applyOrigin(ORIGIN).compose(FluidContainerModel.FLUID_TRANSFORM), false)); // Bake with fluid texture

    // apply brightness and color
    int luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);
    if (luminosity > 0) {
      QuadTransformers.settingEmissivity(luminosity).processInPlace(fluidQuads);
    }
    int color = attributes.getTintColor(fluid);
    if (color != -1) {
      ColoredBlockModel.applyColorQuadTransformer(color).processInPlace(fluidQuads);
    }
    quadConsumer.accept(fluidQuads);''', '''    List<BlockElement> unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(-1, spriteGetter.apply(template)); // Use template as mask
    // TODO: is there anything that can be done about the fluid? to prevent weird offsets?
    Transformation fluidTransform = transforms.applyOrigin(ORIGIN).compose(FLUID_TRANSFORM);
    List<BakedQuad> fluidQuads = UnbakedElementsHelper.bakeElements(unbaked, mat -> fluidSprite, new ModelState() {
      @Override
      public Transformation transformation() {
        return fluidTransform;
      }
    }); // Bake with fluid texture

    // apply brightness and color
    int luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);
    int color = attributes.getTintColor(fluid);
    if (luminosity > 0 || color != -1) {
      fluidQuads = fluidQuads.stream().map(quad -> ColoredBlockModel.withColorAndLight(quad, color, luminosity)).toList();
    }
    quadConsumer.accept(fluidQuads);'''),
            ('  private static final Vector3f ORIGIN = new Vector3f(-0.5f, -0.5f, -0.5f);', '''  private static final Vector3f ORIGIN = new Vector3f(-0.5f, -0.5f, -0.5f);
  /** Scales the fluid slightly out of the container to prevent z-fighting */
  public static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());''')],
     imports=['net.neoforged.neoforge.client.model.UnbakedElementsHelper', 'net.minecraft.client.resources.model.ModelState', 'org.joml.Quaternionf'])

port(T + 'modifiers/ModifierModelMapManager.java',
     pairs=[('''  public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
    // run in the first stage instead of the second stage
    return CompletableFuture.runAsync(() -> {
      this.onResourceManagerReload(resourceManager);
    }, backgroundExecutor).thenCompose(stage::wait);''', '''  public CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor, PreparationBarrier stage, Executor gameExecutor) {
    // run in the first stage instead of the second stage
    ResourceManager resourceManager = sharedState.resourceManager();
    return CompletableFuture.runAsync(() -> {
      this.onResourceManagerReload(resourceManager);
    }, backgroundExecutor).thenCompose(stage::wait);''')])

port(T + 'modifiers/ModifierModelManager.java',
     pairs=[('''  public static void init(RegisterClientReloadListenersEvent manager) {
    manager.registerReloadListener(INSTANCE);''', '''  public static void init(AddClientReloadListenersEvent manager) {
    manager.addListener(TConstruct.getResource("modifier_models"), INSTANCE);''')],
     imports=['net.neoforged.neoforge.client.event.AddClientReloadListenersEvent', 'slimeknights.tconstruct.TConstruct'])

port(T + 'modifiers/PotionModifierModel.java',
     pairs=[('Potion potion = BuiltInRegistries.POTION.get(id);', 'Potion potion = BuiltInRegistries.POTION.getValue(id);')])

port(T + 'modifiers/TrimModifierModel.java',
     pairs=[('''    if (!isLarge && tool.getItem() instanceof ArmorItem armor) {
      int ordinal = armor.getType().ordinal();
      if (ordinal < 4) {
        Armor model = Armor.values()[ordinal];''', '''    Equippable equippable = isLarge ? null : tool.getItem().components().get(DataComponents.EQUIPPABLE);
    if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
      // armor models are ordered helmet to boots, the inverse of the slot index
      int ordinal = 3 - equippable.slot().getIndex();
      if (ordinal >= 0 && ordinal < 4) {
        Armor model = Armor.values()[ordinal];''')],
     imports=['net.minecraft.core.component.DataComponents', 'net.minecraft.world.item.equipment.Equippable', 'net.minecraft.world.entity.EquipmentSlot'])

port(T + 'modifiers/model/TrimModifierModel.java',
     pairs=[('TrimMaterial material = level.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).get(Identifier.tryParse(materialId));', 'TrimMaterial material = level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getValue(Identifier.tryParse(materialId));'),
            ('Identifier path = root.withSuffix("_" + material.assetName());', 'Identifier path = root.withSuffix("_" + material.assets().base().suffix());')])
print('ok')
