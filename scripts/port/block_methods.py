"""Adapts Block overrides whose signature changed between 1.21.1 and 1.21.11, keeping the old parameter names.

Usage: python scripts/port/block_methods.py <file>...

- updateShape: (state, direction, neighborState, LevelAccessor, pos, neighborPos) -> (state, LevelReader, ScheduledTickAccess, pos, direction, neighborPos, neighborState, random)
- entityInside: adds InsideBlockEffectApplier and the intersects flag
- getCloneItemStack: adds the includeData flag
- getAnalogOutputSignal: adds the direction
- propagatesSkylightDown / getLightBlock: only take the state
"""
import re
import sys

from portio import read, write

sys.path.insert(0, __file__.rsplit('scripts', 1)[0] + 'scripts/port')
from fix_by_errors import args_end, split_args  # noqa: E402


def body_range(text, sig_end):
    """Start (after {) and end (index of matching }) of the method body starting after sig_end."""
    open_idx = text.index('{', sig_end)
    depth = 0
    for i in range(open_idx, len(text)):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return open_idx + 1, i
    raise ValueError('unbalanced')


def rewrite_super(body, method, fn):
    out = ''
    pos = 0
    for m in re.finditer(r'super\.' + method + r'\(', body):
        if m.start() < pos:
            continue
        end = args_end(body, m.end() - 1)
        args = split_args(body[m.end():end - 1])
        out += body[pos:m.start()] + 'super.' + method + '(' + ', '.join(fn(args)) + ')'
        pos = end
    return out + body[pos:]


def process(text):
    # updateShape
    sig = re.compile(r'(public|protected) BlockState updateShape\(BlockState (\w+), Direction (\w+), BlockState (\w+), LevelAccessor (\w+), BlockPos (\w+), BlockPos (\w+)\)')
    while True:
        m = sig.search(text)
        if not m:
            break
        vis, state, direction, neighbor_state, level, pos, neighbor_pos = m.groups()
        new_sig = (f'protected BlockState updateShape(BlockState {state}, LevelReader {level}, ScheduledTickAccess scheduledTickAccess, '
                   f'BlockPos {pos}, Direction {direction}, BlockPos {neighbor_pos}, BlockState {neighbor_state}, RandomSource random)')
        start, end = body_range(text, m.end())
        body = text[start:end]
        body = rewrite_super(body, 'updateShape', lambda a: [a[0], a[3], 'scheduledTickAccess', a[4], a[1], a[5], a[2], 'random'] if len(a) == 6 else a)
        body = re.sub(r'\b' + re.escape(level) + r'\.scheduleTick\(', 'scheduledTickAccess.scheduleTick(', body)
        text = text[:m.start()] + new_sig + text[m.end():start] + body + text[end:]
    # entityInside
    sig = re.compile(r'(public|protected) void entityInside\(BlockState (\w+), Level (\w+), BlockPos (\w+), Entity (\w+)\)')
    while True:
        m = sig.search(text)
        if not m:
            break
        vis, state, level, pos, entity = m.groups()
        new_sig = f'protected void entityInside(BlockState {state}, Level {level}, BlockPos {pos}, Entity {entity}, InsideBlockEffectApplier effectApplier, boolean intersects)'
        start, end = body_range(text, m.end())
        body = rewrite_super(text[start:end], 'entityInside', lambda a: a + ['effectApplier', 'intersects'] if len(a) == 4 else a)
        text = text[:m.start()] + new_sig + text[m.end():start] + body + text[end:]
    # getCloneItemStack, 3 argument form
    sig = re.compile(r'public ItemStack getCloneItemStack\((?:LevelReader|BlockGetter) (\w+), BlockPos (\w+), BlockState (\w+)\)')
    while True:
        m = sig.search(text)
        if not m:
            break
        level, pos, state = m.groups()
        new_sig = f'protected ItemStack getCloneItemStack(LevelReader {level}, BlockPos {pos}, BlockState {state}, boolean includeData)'
        start, end = body_range(text, m.end())
        body = rewrite_super(text[start:end], 'getCloneItemStack', lambda a: a + ['includeData'] if len(a) == 3 else a)
        text = text[:m.start()] + new_sig + text[m.end():start] + body + text[end:]
    # getAnalogOutputSignal
    text = re.sub(r'(public|protected) int getAnalogOutputSignal\(BlockState (\w+), Level (\w+), BlockPos (\w+)\)',
                  r'protected int getAnalogOutputSignal(BlockState \2, Level \3, BlockPos \4, Direction direction)', text)
    # state only lighting methods
    text = re.sub(r'(public|protected) boolean propagatesSkylightDown\(BlockState (\w+), BlockGetter \w+, BlockPos \w+\)',
                  r'protected boolean propagatesSkylightDown(BlockState \2)', text)
    text = re.sub(r'(public|protected) int getLightBlock\(BlockState (\w+), BlockGetter \w+, BlockPos \w+\)',
                  r'protected int getLightBlock(BlockState \2)', text)
    return text


IMPORTS = {
    'ScheduledTickAccess': 'import net.minecraft.world.level.ScheduledTickAccess;',
    'LevelReader': 'import net.minecraft.world.level.LevelReader;',
    'RandomSource': 'import net.minecraft.util.RandomSource;',
    'InsideBlockEffectApplier': 'import net.minecraft.world.entity.InsideBlockEffectApplier;',
    'Direction': 'import net.minecraft.core.Direction;',
}

for path in sys.argv[1:]:
    text = read(path)
    new = process(text)
    if new != text:
        for simple, imp in IMPORTS.items():
            if re.search(r'\b' + simple + r'\b', new) and imp not in new:
                new = re.sub(r'^(import [^\n]*\n)', imp + r'\n\1', new, count=1, flags=re.M)
        write(path, new)
        print('updated', path)
