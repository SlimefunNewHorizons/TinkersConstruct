package slimeknights.tconstruct.library.client.model;

import slimeknights.mantle.client.model.util.ModelHelper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterLoaders;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.FluidModifierModel;
import slimeknights.tconstruct.library.client.modifiers.MaterialModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager.ModifierModelRegistrationEvent;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.PotionModifierModel;
import slimeknights.tconstruct.library.client.modifiers.TankModifierModel;
import slimeknights.tconstruct.library.client.modifiers.TrimModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.BannerModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.CompoundModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ConditionalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialHasFallbackModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.TraitModel;
import slimeknights.tconstruct.tools.modules.ranged.ammo.SmashingModule;
import slimeknights.tconstruct.tools.client.OverslimeModifierModel;
import slimeknights.tconstruct.tools.client.ShieldBannerModifierSpriteSource;

import static slimeknights.tconstruct.TConstruct.getResource;

/** Registers the tool, material and modifier models */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class TinkerModelEvents {
  private TinkerModelEvents() {}

  /** Called on mod construct, modifier model loaders are needed before resources load */
  public static void init() {
    ModifierModel.LOADER.register(getResource("empty"), ModifierModel.EMPTY.getLoader());
    ModifierModel.LOADER.register(getResource("compound"), CompoundModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("conditional"), ConditionalModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("trait"), TraitModel.LOADER);
    ModifierModel.LOADER.register(getResource("basic"), NormalModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("dyed"), DyedModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("material"), MaterialModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("potion"), PotionModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("armor_trim"), slimeknights.tconstruct.library.client.modifiers.model.TrimModifierModel.Armor.LOADER);
    ModifierModel.LOADER.register(getResource("custom_trim"), slimeknights.tconstruct.library.client.modifiers.model.TrimModifierModel.Custom.LOADER);
    ModifierModel.LOADER.register(getResource("banner"), BannerModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("fluid"), slimeknights.tconstruct.library.client.modifiers.model.FluidModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("tank"), slimeknights.tconstruct.library.client.modifiers.model.TankModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("material_has_fallback"), MaterialHasFallbackModifierModel.LOADER);
  }

  @SubscribeEvent
  static void addResourceListeners(AddClientReloadListenersEvent event) {
    ModifierModelManager.init(event);
    event.addListener(getResource("modifier_model_maps"), ModifierModelMapManager.INSTANCE);
    DynamicTextureLoader.init(event);
  }

  @SubscribeEvent
  static void registerModelLoaders(RegisterLoaders event) {
    event.register(getResource("material"), MaterialModel.LOADER);
    event.register(getResource("tool"), ToolModel.LOADER);
    // shortcut: these render their base model without fluids or materials, port TankModel, MaterialBlockModel, FluidTextureModel and UniqueGuiModel to show them
    UnbakedModelLoader<UnbakedModel> base = ModelHelper::parseVanilla;
    event.register(getResource("tank"), base);
    event.register(getResource("material_block"), base);
    event.register(getResource("fluid_texture"), base);
    event.register(getResource("gui"), base);
    event.register(getResource("fluid_container"), base);
  }

  @SubscribeEvent
  static void registerSpriteSources(RegisterSpriteSourcesEvent event) {
    event.register(getResource("shield_banner_to_modifier"), ShieldBannerModifierSpriteSource.CODEC);
  }

  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(getResource("material"), MaterialModel.Unbaked.MAP_CODEC);
    event.register(getResource("tool"), ToolModel.Unbaked.MAP_CODEC);
    event.register(getResource("overrides"), OverridesItemModel.Unbaked.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerModifierModels(ModifierModelRegistrationEvent event) {
    event.registerModel(getResource("normal"), NormalModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("overslime"), OverslimeModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("fluid"), FluidModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("tank"), TankModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("material"), MaterialModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("dyed"), DyedModifierModel.UNBAKED_INSTANCE);
    // trim shows up as valid on every tool, skip to reduce memory overhead on tools using the new system - add it using the new system if you want it
    event.registerModel(getResource("trim"), TrimModifierModel.UNBAKED_INSTANCE);
    ModifierModelMapManager.legacyBlacklist(TrimModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("potion"), PotionModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("smashing_fluid"), new FluidModifierModel.Unbaked(SmashingModule.TANK_HELPER));
  }

  @SubscribeEvent
  static void clientSetup(FMLClientSetupEvent event) {
    ToolModel.registerSmallTool(TinkerItemDisplays.MELTER.getValue());
    ToolModel.registerSmallTool(TinkerItemDisplays.CASTING_BASIN.getValue());
    ToolModel.registerSmallTool(TinkerItemDisplays.CASTING_TABLE.getValue());
  }
}
