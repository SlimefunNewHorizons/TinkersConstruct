package slimeknights.tconstruct.fluids;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;

@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class FluidClientEvents extends ClientEventBase {
  @SubscribeEvent
  static void clientSetup(final FMLClientSetupEvent event) {
    setTranslucent(TinkerFluids.honey);
    // slime
    setTranslucent(TinkerFluids.earthSlime);
    setTranslucent(TinkerFluids.skySlime);
    setTranslucent(TinkerFluids.enderSlime);
    // molten
    setTranslucent(TinkerFluids.moltenDiamond);
    setTranslucent(TinkerFluids.moltenEmerald);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.liquidSoul);
    setTranslucent(TinkerFluids.moltenSoulsteel);
    setTranslucent(TinkerFluids.moltenAmethyst);
  }

  private static void setTranslucent(FlowingFluidObject<?> fluid) {
    ItemBlockRenderTypes.setRenderLayer(fluid.getStill(), ChunkSectionLayer.TRANSLUCENT);
    ItemBlockRenderTypes.setRenderLayer(fluid.getFlowing(), ChunkSectionLayer.TRANSLUCENT);
  }
}
