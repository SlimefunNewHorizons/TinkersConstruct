package slimeknights.tconstruct.shared.client;

/**
 * Client extensions removed from common classes, as NeoForge 21.x registers them through RegisterClientExtensionsEvent.
 * shortcut: not ported yet, register each in a client event handler while porting the client code.
 */
final class PendingClientExtensions {
  private PendingClientExtensions() {}

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
