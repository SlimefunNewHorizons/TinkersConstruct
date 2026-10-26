package slimeknights.tconstruct.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.item.IModifiable;

/**
 * Tool data is edited in place inside its custom data component, so a copied tool must not share that component with the original.
 * shortcut: in place editing of {@link CustomData}, move tool data to dedicated immutable components to drop this mixin.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
  @Inject(method = "copy", at = @At("RETURN"))
  private void tconstruct$unshareToolData(CallbackInfoReturnable<ItemStack> cir) {
    ItemStack copy = cir.getReturnValue();
    if (copy.getItem() instanceof IModifiable) {
      CustomData data = copy.get(DataComponents.CUSTOM_DATA);
      if (data != null) {
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(data.tag));
      }
    }
  }
}
