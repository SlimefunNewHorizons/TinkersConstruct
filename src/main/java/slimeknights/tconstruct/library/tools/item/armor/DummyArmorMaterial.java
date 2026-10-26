package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.Map;

/** Wrapper around the immutable armor material value used by modifiable armor. Stats come from tool definitions, so the material only holds the sound and ID */
@Getter
public class DummyArmorMaterial implements IdAwareObject {
  private final Identifier id;
  private final SoundEvent equipSound;
  private final ArmorMaterial armorMaterial;

  public DummyArmorMaterial(Identifier id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;
    this.armorMaterial = new ArmorMaterial(0, Map.of(), 0, Holder.direct(equipSound), 0, 0,
      TagKey.create(Registries.ITEM, id), ResourceKey.create(EquipmentAssets.ROOT_ID, id));
  }
}
