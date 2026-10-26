"""Converts list based appendHoverText overrides to the 1.21.5 consumer signature.

Usage: python scripts/port/tooltip_fix.py <file>...

The body keeps writing to a local list named like the old parameter, flushed to the consumer in a finally block
so early returns still emit their lines.
"""
import re
import sys

from portio import read, write

SIG = re.compile(r'public void appendHoverText\(ItemStack (\w+), (?:Item\.)?TooltipContext (\w+), List<Component> (\w+), TooltipFlag (\w+)\) \{')


def body_end(text, open_idx):
    depth = 0
    i = open_idx
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError('unbalanced braces')


def convert(text):
    out = text
    pos = 0
    while True:
        m = SIG.search(out, pos)
        if not m:
            return out
        stack, context, tooltip, flag = m.groups()
        open_idx = m.end() - 1
        close_idx = body_end(out, open_idx)
        body = out[open_idx + 1:close_idx]
        nl = '\r\n' if '\r\n' in out else '\n'
        body = re.sub(r'super\.appendHoverText\(' + stack + r', ' + context + r', ' + tooltip + r', ' + flag + r'\)',
                      f'super.appendHoverText({stack}, {context}, display, {tooltip}::add, {flag})', body)
        indent = '    '
        inner = body.strip('\r\n')
        inner = nl.join((indent + line) if line.strip() else line for line in inner.split(nl))
        new = (f'public void appendHoverText(ItemStack {stack}, Item.TooltipContext {context}, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag {flag}) {{{nl}'
               f'    List<Component> {tooltip} = new ArrayList<>();{nl}'
               f'    try {{{nl}{inner}{nl}'
               f'    }} finally {{{nl}'
               f'      {tooltip}.forEach(tooltipAdder);{nl}'
               f'    }}{nl}  }}')
        out = out[:m.start()] + new + out[close_idx + 1:]
        pos = m.start() + len(new)


IMPORTS = ['import net.minecraft.world.item.component.TooltipDisplay;', 'import java.util.function.Consumer;',
           'import java.util.ArrayList;', 'import java.util.List;', 'import net.minecraft.world.item.Item;']

for path in sys.argv[1:]:
    text = read(path)
    new = convert(text)
    if new != text:
        for imp in IMPORTS:
            if imp not in new:
                new = re.sub(r'^(import [^\n]*\n)', imp + r'\n\1', new, count=1, flags=re.M)
        write(path, new)
        print('updated', path)
