package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.ArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

import java.util.function.Consumer;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  /** Name of the armor model, used by the client */
  private final Identifier name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties, Identifier name) {
    super(material, slot, properties);
    this.name = name;
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties, ToolDefinition toolDefinition, Identifier name) {
    super(material.getArmorMaterial(), slot, properties, toolDefinition);
    this.name = name;
  }

  public MultilayerArmorItem(ArmorMaterial material, ArmorType slot, Properties properties, ToolDefinition toolDefinition, Identifier name) {
    super(material, slot, properties, toolDefinition);
    this.name = name;
  }

  /** Gets the name of the armor model, used by the client */
  public Identifier getArmorName() {
    return name;
  }
}
