package slimeknights.mantle.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.mantle.registration.RegistrationContext;

import javax.annotation.Nullable;

/** Fills in the block ID from {@link RegistrationContext}, as properties must know their ID since 1.21.2 */
@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockPropertiesMixin {
  @Shadow
  @Nullable
  private ResourceKey<Block> id;

  @Inject(method = {"effectiveDrops", "effectiveDescriptionId"}, at = @At("HEAD"))
  private void mantle$contextId(CallbackInfoReturnable<?> cir) {
    ResourceKey<Block> key = RegistrationContext.current(Registries.BLOCK);
    if (key != null) {
      this.id = key;
    }
  }
}
