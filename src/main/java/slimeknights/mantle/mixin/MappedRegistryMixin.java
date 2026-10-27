package slimeknights.mantle.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.mantle.util.TagHelper;

/** Records tags before they bind, so recipe conditions can check tag contents like before 1.21.2 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin {
  @Inject(method = "prepareTagReload", at = @At("HEAD"))
  private void mantle$recordPendingTags(TagLoader.LoadResult<?> result, CallbackInfoReturnable<Registry.PendingTags<?>> cir) {
    TagHelper.setPendingTags(result.key(), result.tags());
  }
}
