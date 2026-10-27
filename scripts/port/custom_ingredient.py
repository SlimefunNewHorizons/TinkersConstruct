"""1.21.2: ICustomIngredient#getItems became items() returning holders plus display(). Keeps getItems as a plain stack helper.

Usage: python scripts/port/custom_ingredient.py <file>...
"""
import re
import sys

from portio import read, write

OLD = re.compile(r'  @Override(\r?\n)  public Stream<ItemStack> getItems\(\) \{')
IMPORTS = ['net.minecraft.core.Holder', 'net.minecraft.world.item.Item', 'net.minecraft.world.item.crafting.display.SlotDisplay']

for path in sys.argv[1:]:
  text = read(path)
  m = OLD.search(text)
  if not m:
    print('skipped', path)
    continue
  nl = m.group(1)
  new = nl.join([
    '  @Override',
    '  public Stream<Holder<Item>> items() {',
    '    return getItems().map(ItemStack::getItemHolder).distinct();',
    '  }',
    '',
    '  @Override',
    '  public SlotDisplay display() {',
    '    return new SlotDisplay.Composite(getItems().<SlotDisplay>map(SlotDisplay.ItemStackSlotDisplay::new).toList());',
    '  }',
    '',
    '  /** Gets the stacks to display for this ingredient */',
    '  public Stream<ItemStack> getItems() {'])
  text = text[:m.start()] + new + text[m.end():]
  for imp in IMPORTS:
    line = 'import ' + imp + ';'
    if line not in text:
      text = re.sub(r'^(import [^\n]*\n)', line + nl + r'\1', text, count=1, flags=re.M)
  write(path, text)
  print('updated', path)
