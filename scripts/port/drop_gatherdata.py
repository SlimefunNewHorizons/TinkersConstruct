"""One-off: removes the stubbed gatherData listeners. GatherDataEvent is abstract since NeoForge 21.4 and datagen is not ported yet.

Usage: python scripts/port/drop_gatherdata.py <file>...
"""
import re
import sys

from portio import read, write

STUB = re.compile(r'(\r?\n)  @SubscribeEvent\r?\n  void gatherData\(final GatherDataEvent event\) \{\r?\n\s*// Runtime-first 1\.21 port: providers are migrated separately\.\r?\n  \}\r?\n')

for path in sys.argv[1:]:
    text = read(path)
    new = STUB.sub(r'\1', text)
    if 'GatherDataEvent' not in STUB.sub('', new).replace('import net.neoforged.neoforge.data.event.GatherDataEvent;', ''):
        new = re.sub(r'^import net\.neoforged\.neoforge\.data\.event\.GatherDataEvent;\r?\n', '', new, flags=re.M)
    if new != text:
        write(path, new)
        print('updated', path)
    else:
        print('unchanged', path)
