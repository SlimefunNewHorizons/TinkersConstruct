package slimeknights.tconstruct.shared.client;

import slimeknights.tconstruct.library.client.TinkerRenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerClient;

/** Client only entry point, keeps client classes out of the common mod constructor */
@Mod(value = TConstruct.MOD_ID, dist = Dist.CLIENT)
public class TConstructClient {
  public TConstructClient(IEventBus bus) {
    TinkerClient.onConstruct();
    bus.addListener(TinkerRenderTypes::registerPipelines);
    // shortcut: JSON Things tool models are not registered, PluginClient waits for a JSON Things build for 1.21.11
  }
}
