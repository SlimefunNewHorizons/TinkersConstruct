package slimeknights.mantle.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.mantle.registration.RegistrationContext;

import javax.annotation.Nullable;

/** Fills in the item ID from {@link RegistrationContext}, as properties must know their ID since 1.21.2 */
@Mixin(Item.Properties.class)
public abstract class ItemPropertiesMixin {
  @Shadow
  @Nullable
  private ResourceKey<Item> id;

  @Inject(method = "effectiveDescriptionId", at = @At("HEAD"), cancellable = true)
  private void mantle$contextDescriptionId(CallbackInfoReturnable<String> cir) {
    ResourceKey<Item> key = RegistrationContext.current(Registries.ITEM);
    boolean blockItem = RegistrationContext.consumeBlockItem();
    if (key != null) {
      this.id = key;
      if (blockItem) {
        cir.setReturnValue(Util.makeDescriptionId("block", key.identifier()));
      }
    }
  }

  @Inject(method = "effectiveModel", at = @At("HEAD"))
  private void mantle$contextModel(CallbackInfoReturnable<?> cir) {
    ResourceKey<Item> key = RegistrationContext.current(Registries.ITEM);
    if (key != null) {
      this.id = key;
    }
  }
}
