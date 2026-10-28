package slimeknights.tconstruct.shared.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.utils.PotionUtils;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

/** Client extensions for Tinkers effects and fluids, registered here as NeoForge 21.x removed initializeClient */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class TinkerClientExtensions {
  private TinkerClientExtensions() {}

  @SubscribeEvent
  static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    // effects with special icons
    event.registerMobEffect(new CarryExtension(), TinkerGadgets.carryEffect.get());
    event.registerMobEffect(new HelmetChargingExtension(), TinkerModifiers.helmetCharging.get());
    // hidden effects
    for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
      if (effect instanceof TinkerEffect tinkerEffect && !tinkerEffect.isShown() && !event.isMobEffectRegistered(effect)) {
        event.registerMobEffect(new IClientMobEffectExtensions() {
          @Override
          public boolean isVisibleInInventory(MobEffectInstance instance) {
            return false;
          }

          @Override
          public boolean isVisibleInGui(MobEffectInstance instance) {
            return false;
          }
        }, effect);
      }
    }

    // potion fluid takes its color from the stack
    event.registerFluidType(new ClientTextureFluidType(TinkerFluids.potion.getType()) {
      @Override
      public int getTintColor(FluidStack stack) {
        PotionContents contents = PotionUtils.getPotionContents(stack);
        if (contents.equals(PotionContents.EMPTY)) {
          return getTintColor();
        }
        return contents.getColor() | 0xFF000000;
      }
    }, TinkerFluids.potion.getType());
  }

  /** Piggyback carry effect, icon changes with the level */
  private static class CarryExtension implements IClientMobEffectExtensions {
    private static final Identifier[] ICONS = {
      TConstruct.getResource("mob_effect/carry"),
      TConstruct.getResource("mob_effect/carry_2"),
      TConstruct.getResource("mob_effect/carry_3")
    };

    /** Common logic to render the icon */
    private static void renderIcon(MobEffectInstance effect, GuiGraphics graphics, int x, int y) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICONS[Math.min(effect.getAmplifier(), 2)], x, y, 18, 18);
    }

    @Override
    public boolean renderInventoryIcon(MobEffectInstance effect, AbstractContainerScreen<?> gui, GuiGraphics graphics, int x, int y, int z) {
      renderIcon(effect, graphics, x, y + 7);
      return true;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance effect, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
      renderIcon(effect, graphics, x + 3, y + 3);
      return true;
    }
  }

  /** Helmet charging effect, shows the draw time as a bar */
  private static class HelmetChargingExtension implements IClientMobEffectExtensions {
    private static final Identifier ICON = TConstruct.getResource("mob_effect/helmet_charging");
    private static final Identifier BAR = TConstruct.getResource("mob_effect/helmet_charging_bar");

    @Override
    public boolean isVisibleInInventory(MobEffectInstance instance) {
      return false;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
      // start by drawing the original texture, skip alpha
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON, x + 3, y + 3, 18, 18);

      // if the helmet has a drawtime, render the extra bar
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
          // if we have a drawtime, draw the bar overlaying the icon
          int duration = instance.getDuration();
          int drawtime = ModifierUtil.getPersistentInt(helmet, GeneralInteractionModifierHook.KEY_DRAWTIME, 0);
          int dd = drawtime + 20;
          if (drawtime > 0 && duration < dd) {
            int height;
            if (duration < 20) {
              height = 18;
            } else {
              height = (dd - duration) * 18 / drawtime;
            }
            int yOffset = (18 - height);
            graphics.enableScissor(x + 3, y + 3 + yOffset, x + 21, y + 21);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR, x + 3, y + 3, 18, 18);
            graphics.disableScissor();
          }
        }
      }
      return true;
    }
  }
}
