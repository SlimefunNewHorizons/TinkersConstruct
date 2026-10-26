"""One-off: removes 1.20 armor texture overrides and adapts armor material holders to 1.21.5 (ArmorMaterial is a plain record)."""
import re
import sys

from portio import read, write

for path in sys.argv[1:]:
    text = read(path)
    old = text
    # getArmorTexture(ItemStack, Entity, EquipmentSlot, ArmorMaterial.Layer, boolean) no longer exists, armor rendering goes through client extensions
    text = re.sub(r'\r?\n  @Nullable\r?\n  @Override\r?\n  public Identifier getArmorTexture\([^)]*\) \{\r?\n[^}]*\}\r?\n', '\n', text)
    # initializeClient is no longer an item hook, the client registers extensions from it in RegisterClientExtensionsEvent
    text = re.sub(r'(\r?\n)  @Override(\r?\n  public void initializeClient\()', r'\2', text)
    text = text.replace('Holder<ArmorMaterial>', 'ArmorMaterial')
    if text != old:
        write(path, text)
        print('updated', path)
