"""Converts Block#onRemove overrides (removed in 1.21.5) to Mantle's IBlockEntityRemovalHandler hook.

Usage: python scripts/port/on_remove.py <file>...
"""
import re
import sys

from portio import read, write

SIG = re.compile(r'(\r?\n)((?:  @\w+(?:\([^)]*\))?\r?\n)*)  public void onRemove\(BlockState (\w+), Level (\w+), BlockPos (\w+), BlockState (\w+), boolean \w+\) \{')
IMPORT = 'import slimeknights.mantle.block.IBlockEntityRemovalHandler;'

for path in sys.argv[1:]:
    text = read(path)
    m = SIG.search(text)
    if not m:
        continue
    nl, annotations, state, level, pos, new_state = m.groups()
    # keep annotations other than the deprecation ones
    kept = ''.join(a + nl for a in annotations.split(nl) if a.strip() and 'eprecat' not in a)
    if '@Override' not in kept:
        kept = '  @Override' + nl + kept
    header = f'{nl}{kept}  public void onBlockEntityRemoved(BlockState {state}, Level {level}, BlockPos {pos}, BlockState {new_state}) {{'
    text = text[:m.start()] + header + text[m.end():]
    # drop the super call, the chunk removes the block entity itself
    text = re.sub(r'\r?\n\s*super\.onRemove\([^;]*\);', '', text)
    # implement the interface, unless the class extends a block already handling it
    cls = re.search(r'public (?:abstract )?class \w+(?:<[^{]*?>)? extends ([\w.<>, ]+?)(?: implements ([^{]+))?\s*\{', text)
    if cls and cls.group(1).strip() not in ('InventoryBlock',):
        if cls.group(2):
            text = text[:cls.end(2)] + ', IBlockEntityRemovalHandler' + text[cls.end(2):]
        else:
            text = text[:cls.end(1)] + ' implements IBlockEntityRemovalHandler' + text[cls.end(1):]
        if IMPORT not in text and 'package slimeknights.mantle.block;' not in text:
            text = re.sub(r'^(import [^\n]*\n)', IMPORT + r'\n\1', text, count=1, flags=re.M)
    write(path, text)
    print('updated', path)
