package slimeknights.tconstruct.tables.network;

import slimeknights.tconstruct.tables.menu.ITabbedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.network.PacketContext;
import slimeknights.mantle.network.packet.IThreadsafePacket;

public class UpdateStationScreenPacket implements IThreadsafePacket {
  public static final UpdateStationScreenPacket INSTANCE = new UpdateStationScreenPacket();

  private UpdateStationScreenPacket() {}

  @Override
  public void encode(FriendlyByteBuf packetBuffer) {}

  @Override
  public void handleThreadsafe(PacketContext context) {
    HandleClient.handle();
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle() {
      Screen screen = Minecraft.getInstance().screen;
      if (screen != null) {
        if (screen instanceof ITabbedScreen) {
          ((ITabbedScreen) screen).updateDisplay();
        }
      }
    }
  }
}
