package slimeknights.tconstruct.shared.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.plugin.jsonthings.PluginClient;
import slimeknights.tconstruct.shared.TinkerClient;

/** Client only entry point, keeps client classes out of the common mod constructor */
@Mod(value = TConstruct.MOD_ID, dist = Dist.CLIENT)
public class TConstructClient {
  public TConstructClient(IEventBus bus) {
    TinkerClient.onConstruct();
    if (ModList.get().isLoaded("jsonthings")) {
      PluginClient.init();
    }
  }
}
