"""1.21.2 loot context parameters became generic context keys. Usage: python scripts/port/context_keys.py <file>..."""
import re
import sys

from portio import read, write

for path in sys.argv[1:]:
    s = read(path)
    o = s
    s = re.sub(r'^import net\.minecraft\.world\.level\.storage\.loot\.parameters\.LootContextParam;', 'import net.minecraft.util.context.ContextKey;', s, flags=re.M)
    s = re.sub(r'^import net\.minecraft\.world\.level\.storage\.loot\.parameters\.LootContextParamSet;', 'import net.minecraft.util.context.ContextKeySet;', s, flags=re.M)
    s = re.sub(r'\bLootContextParamSet\b', 'ContextKeySet', s)
    s = re.sub(r'\bLootContextParam\b', 'ContextKey', s)
    s = re.sub(r'\.getRequired\(\)', '.required()', s)
    s = re.sub(r'\.getAllowed\(\)', '.allowed()', s)
    if s != o:
        write(path, s)
        print('updated', path)
