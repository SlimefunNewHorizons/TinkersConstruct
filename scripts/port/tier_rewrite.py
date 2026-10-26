"""One-off: Tier/Tiers -> ToolMaterial rewrite (1.21.2). Usage: python tier_rewrite.py <file>..."""
import re
import sys
import time

for p in sys.argv[1:]:
    s = open(p, encoding='utf-8', newline='').read()
    o = s
    s = re.sub(r'^import net\.minecraft\.world\.item\.Tiers;\r?\n', '', s, flags=re.M)
    s = re.sub(r'^import net\.minecraft\.world\.item\.Tier;', 'import net.minecraft.world.item.ToolMaterial;', s, flags=re.M)
    s = re.sub(r'\bTiers\.(WOOD|STONE|IRON|DIAMOND|GOLD|NETHERITE)\b', r'ToolMaterial.\1', s)
    s = re.sub(r'(?<![\w.])Tiers?\b(?!\s*\()', 'ToolMaterial', s)
    if 'ToolMaterial' in s and 'import net.minecraft.world.item.ToolMaterial;' not in s:
        s = re.sub(r'^(import [^\n]*\n)', r'import net.minecraft.world.item.ToolMaterial;\n\1', s, count=1, flags=re.M)
    if s != o:
        for attempt in range(5):
            try:
                with open(p, 'w', encoding='utf-8', newline='') as f:
                    f.write(s)
                print('updated', p)
                break
            except OSError as e:
                print('retry', p, e)
                time.sleep(1)
