"""1.21.2: Block#neighborChanged receives an Orientation instead of the neighbor position. Usage: python scripts/port/neighbor_changed.py <file>..."""
import re
import sys

from portio import read, write

for path in sys.argv[1:]:
    s = read(path)
    m = re.search(r'public void neighborChanged\(BlockState (\w+), Level (\w+), BlockPos (\w+), Block (\w+), BlockPos (\w+), boolean (\w+)\)', s)
    if not m:
        continue
    state, level, pos, block, frm, moving = m.groups()
    s = s[:m.start()] + f'protected void neighborChanged(BlockState {state}, Level {level}, BlockPos {pos}, Block {block}, @Nullable Orientation orientation, boolean {moving})' + s[m.end():]
    s = re.sub(r'super\.neighborChanged\((\w+), (\w+), (\w+), (\w+), ' + frm + r', (\w+)\)', r'super.neighborChanged(\1, \2, \3, \4, orientation, \5)', s)
    for imp in ('import net.minecraft.world.level.redstone.Orientation;', 'import javax.annotation.Nullable;'):
        if imp not in s:
            s = re.sub(r'^(import [^\n]*\n)', imp + '\n' + r'\1', s, count=1, flags=re.M)
    write(path, s)
    print('updated', path)
