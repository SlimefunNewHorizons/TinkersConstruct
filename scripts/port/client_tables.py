"""Ports the Tinkers table screens to 1.21.11"""
import re

from portio import add_import, edit, read, write

I = 'src/main/java/slimeknights/tconstruct/tables/client/inventory/'

# screen key state moved to Minecraft
for path in [I + 'ToolTableScreen.java', I + 'TinkerStationScreen.java']:
  text = read(path)
  text = re.sub(r'(?<![\w.])(hasControlDown|hasShiftDown|hasAltDown)\(\)', r'Minecraft.getInstance().\1()', text)
  write(path, text)

edit(I + 'BaseTabbedScreen.java', [
  ('''  protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
    return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)''', '''  protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
    return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop)'''),
])

edit(I + 'ToolTableScreen.java', [
  ('''      Quaternionf pose = new Quaternionf().rotationXYZ((float)Math.toRadians(25), 0.0F, (float)Math.PI).rotateY(this.armorStandAngle);
      InventoryScreen.renderEntityInInventory(graphics, this.armorStandX, this.armorStandY, this.armorStandScale, ARMOR_STAND_TRANSLATION, pose, null, this.armorStandPreview);
''', '''      // shortcut: entities in GUIs only take a yaw and pitch since 1.21.6, so the stand is tilted like the vanilla inventory player instead of the old fixed pose
      int size = this.armorStandScale;
      float yaw = (float) Math.toDegrees(this.armorStandAngle) / 20f;
      InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, this.armorStandX - size, this.armorStandY - size * 2, this.armorStandX + size, this.armorStandY + size / 2, size, 0.0625f, yaw, -0.4f, this.armorStandPreview);
'''),
  ('''        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
          this.armorStandPreview.setItemSlot(armor.getEquipmentSlot(), copy);
        } else {''', '''        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
          this.armorStandPreview.setItemSlot(equippable.slot(), copy);
        } else {'''),
  ('''      List<Component> list = new ArrayList<>();
      result.getItem().appendHoverText(result, Item.TooltipContext.of(Minecraft.getInstance().level), list, Default.NORMAL);
      tinkerInfo.setText(list);''', '''      // the first line is the name, already shown as the caption
      List<Component> list = new ArrayList<>(result.getTooltipLines(Item.TooltipContext.of(Minecraft.getInstance().level), Minecraft.getInstance().player, Default.NORMAL));
      if (!list.isEmpty()) {
        list.remove(0);
      }
      tinkerInfo.setText(list);'''),
  ('import net.minecraft.world.item.ArmorItem;\n', 'import net.minecraft.core.component.DataComponents;\nimport net.minecraft.world.item.equipment.Equippable;\n'),
])
print('ok')
