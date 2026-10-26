package slimeknights.mantle.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import slimeknights.mantle.registration.RegistrationContext;

/** Marks block item construction so {@link ItemPropertiesMixin} uses the block description ID, as vanilla now requires opting in through the properties */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
  @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;<init>(Lnet/minecraft/world/item/Item$Properties;)V"))
  private static Item.Properties mantle$markBlockItem(Item.Properties properties) {
    RegistrationContext.markBlockItem();
    return properties;
  }
}
