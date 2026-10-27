package slimeknights.tconstruct.library.tools.capability;

import slimeknights.mantle.util.ValueIOHelper;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import slimeknights.tconstruct.common.network.SyncPersistentDataPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.tools.TinkerModifiers;

/** Persistent modifier data stored on entities through NeoForge 1.21 data attachments. */
public final class PersistentDataCapability {
  private PersistentDataCapability() {}

  /** Creates the registered attachment type. */
  public static AttachmentType<ModDataNBT> createAttachmentType() {
    return AttachmentType.builder(ModDataNBT::new)
      .serialize(new IAttachmentSerializer<ModDataNBT>() {
        @Override
        public ModDataNBT read(IAttachmentHolder holder, ValueInput input) {
          return ModDataNBT.readFromNBT(ValueIOHelper.read(input));
        }

        @Override
        public boolean write(ModDataNBT attachment, ValueOutput output) {
          ValueIOHelper.write(output, attachment.getCopy());
          return true;
        }
      })
      .copyOnDeath()
      .build();
  }

  private static AttachmentType<ModDataNBT> type() {
    return TinkerModifiers.persistentDataAttachment.get();
  }

  /** Gets the entity data, creating an empty attachment on first access. */
  public static ModDataNBT getOrWarn(Entity entity) {
    return entity.getData(type());
  }

  /** Registers lifecycle syncing for player attachments. */
  public static void register() {
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerRespawn);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerChangeDimension);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerLoggedIn);
  }

  private static void sync(Player player) {
    player.getExistingData(type()).ifPresent(data ->
      TinkerNetwork.getInstance().sendTo(new SyncPersistentDataPacket(data.getCopy()), player));
  }

  private static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    sync(event.getEntity());
  }

  private static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    sync(event.getEntity());
  }

  private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    sync(event.getEntity());
  }
}
