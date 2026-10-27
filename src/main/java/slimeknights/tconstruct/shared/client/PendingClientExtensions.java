package slimeknights.tconstruct.shared.client;

/**
 * Client extensions removed from common classes, as NeoForge 21.x registers them through RegisterClientExtensionsEvent.
 * shortcut: not ported yet, register each in a client event handler while porting the client code.
 */
final class PendingClientExtensions {
  private PendingClientExtensions() {}

  // from slimeknights/mantle/fluid/InvertedFluidType.java
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
      consumer.accept(new ClientInvertedFluidType(this));
    }

  // from slimeknights/mantle/fluid/TextureFluidType.java
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
      consumer.accept(new ClientTextureFluidType(this));
    }

  // from slimeknights/tconstruct/common/TinkerEffect.java
  public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
      consumer.accept(new IClientMobEffectExtensions() {
        @Override
        public boolean isVisibleInInventory(MobEffectInstance effect) {
          return show;
        }

        @Override
        public boolean isVisibleInGui(MobEffectInstance effect) {
          return show;
        }
      });
    }



  // from slimeknights/tconstruct/fluids/fluids/PotionFluidType.java
  public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
      consumer.accept(new ClientTextureFluidType(this) {
        /**
         * Gets the color, based on {@link PotionUtils#getColor(ItemStack)}
         * @param stack  Fluid stack instance
         * @return  Color for the fluid
         */
        @Override
        public int getTintColor(FluidStack stack) {
          PotionContents contents = PotionUtils.getPotionContents(stack);
          if (contents.equals(PotionContents.EMPTY)) {
            return getTintColor();
          }
          return contents.getColor() | 0xFF000000;
        }
      });
    }



  // from slimeknights/tconstruct/gadgets/item/PiggyBackPackItem.java
    @Override
      public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
          private final Minecraft mc = Minecraft.getInstance();
          private static final Identifier[] ICONS = {
            TConstruct.getResource("carry"),
            TConstruct.getResource("carry_2"),
            TConstruct.getResource("carry_3")
          };

          /** Common logic to render the icon */
          private void renderIcon(MobEffectInstance effect, GuiGraphics graphics, int x, int y) {
            int amplifier = effect.getAmplifier();
            if (amplifier > 2) {
              amplifier = 2;
            }
            // Effect icons are stored in the mob-effect atlas, not the GUI atlas
            // used by GuiGraphics.blitSprite(Identifier).
            MobEffectTextureManager textures = mc.getMobEffectTextures();
            TextureAtlasSprite sprite = textures.getSprite(ICONS[amplifier]);
            graphics.blit(x, y, 0, 18, 18, sprite);
          }

          @Override
          public boolean renderInventoryIcon(MobEffectInstance effect, EffectRenderingInventoryScreen<?> gui, GuiGraphics graphics, int x, int y, int z) {
            renderIcon(effect, graphics, x, y + 7);
            return true;
          }

          @Override
          public boolean renderGuiIcon(MobEffectInstance effect, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
            renderIcon(effect, graphics, x + 3, y + 3);
            return true;
          }
        });
      }


  // from slimeknights/tconstruct/library/tools/item/armor/MultilayerArmorItem.java
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new ArmorModelDispatcher() {
        @Override
        protected Identifier getName() {
          return name;
        }
      });
    }

  // from slimeknights/tconstruct/library/tools/item/ranged/ModifiableCrossbowItem.java
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(ModifiableCrossbowClientExtension.INSTANCE);
    }

  // from slimeknights/tconstruct/tools/item/SlimeskullItem.java
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new ArmorModelDispatcher() {
        @Override
        protected Identifier getName() {
          return name;
        }

        @Nonnull
        @Override
        public Model getGenericArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
          return SlimeskullArmorModel.INSTANCE.setup(living, stack, original, getModel(stack));
        }
      });
    }

  // from slimeknights/tconstruct/tools/modifiers/effect/HelmetChargingEffect.java
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
      consumer.accept(new IClientMobEffectExtensions() {
        private static final Identifier BAR_KEY = TConstruct.getResource("helmet_charging_bar");
        private final Minecraft mc = Minecraft.getInstance();

        @Override
        public boolean isVisibleInInventory(MobEffectInstance instance) {
          return false;
        }

        @Override
        public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
          // start by drawing the original texture, skip alpha
          MobEffectTextureManager textures = mc.getMobEffectTextures();
          TextureAtlasSprite sprite = textures.get(instance.getEffect());
          graphics.blit(x + 3, y + 3, 0, 18, 18, sprite);

          // if the helmet has a drawtime, render the extra bar
          if (mc.player != null) {
            ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
              // if we have a drawtime, draw the bar overlaying the icon
              int duration = instance.getDuration();
              int drawtime = ModifierUtil.getPersistentInt(helmet, GeneralInteractionModifierHook.KEY_DRAWTIME, 0);
              int dd = drawtime + 20;
              if (drawtime > 0 && duration < dd) {
                sprite = textures.getSprite(BAR_KEY);
                int height;
                if (duration < 20) {
                  height = 18;
                } else {
                  height = (dd - duration) * 18 / drawtime;
                }
                int yOffset = (18 - height);
                graphics.enableScissor(x + 3, y + 3 + yOffset, x + 21, y + 21);
                graphics.blit(x + 3, y + 3, 0, 18, 18, sprite);
                graphics.disableScissor();
              }
            }
          }
          return true;
        }
      });
    }

}
