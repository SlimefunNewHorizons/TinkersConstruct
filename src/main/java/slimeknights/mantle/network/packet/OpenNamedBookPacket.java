package slimeknights.mantle.network.packet;

import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.item.AbstractBookItem;
import lombok.AllArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.network.PacketContext;

@AllArgsConstructor
public class OpenNamedBookPacket implements IThreadsafePacket {
  private final Identifier book;

  public OpenNamedBookPacket(FriendlyByteBuf buffer) {
    this.book = buffer.readIdentifier();
  }

  @Override
  public void encode(FriendlyByteBuf buf) {
    buf.writeIdentifier(book);
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    // the opener reports missing books to the player
    AbstractBookItem.opener.open(book, null, ItemStack.EMPTY);
  }
}
