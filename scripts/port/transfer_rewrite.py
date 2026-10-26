"""Rewrites legacy NeoForge item/fluid capability calls to slimeknights.mantle.transfer.TransferHelper (NeoForge 1.21.9 transfer API).

Usage: python scripts/port/transfer_rewrite.py <src-root>
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from fix_by_errors import args_end, split_args, write  # noqa: E402

IMPORT = 'import slimeknights.mantle.transfer.TransferHelper;'


def receiver_start(text, end):
    """Finds the start of the expression ending right before index end (exclusive), e.g. foo.bar(baz)."""
    i = end
    while i > 0:
        ch = text[i - 1]
        if ch == ')':
            depth = 0
            j = i - 1
            while j >= 0:
                if text[j] == ')':
                    depth += 1
                elif text[j] == '(':
                    depth -= 1
                    if depth == 0:
                        break
                j -= 1
            i = j
        elif ch.isalnum() or ch in '_.':
            i -= 1
        else:
            break
    return i


QUERIES = {
    'Capabilities.FluidHandler.BLOCK': 'getFluidHandler',
    'Capabilities.ItemHandler.BLOCK': 'getItemHandler',
    'Capabilities.FluidHandler.ITEM': 'getFluidHandler',
    'Capabilities.ItemHandler.ITEM': 'getItemHandler',
    'Capabilities.ItemHandler.ENTITY_AUTOMATION': 'getItemHandler',
}
REGISTER = {
    ('registerBlockEntity', 'Capabilities.FluidHandler.BLOCK'): 'registerFluid',
    ('registerBlockEntity', 'Capabilities.ItemHandler.BLOCK'): 'registerItems',
    ('registerItem', 'Capabilities.FluidHandler.ITEM'): 'registerItemFluid',
    ('registerItem', 'Capabilities.ItemHandler.ITEM'): 'registerItemItems',
}


def rewrite(text):
    out = text
    pos = 0
    while True:
        m = re.compile(r'\.(getCapability|registerBlockEntity|registerItem)\s*\(').search(out, pos)
        if not m:
            break
        open_idx = m.end() - 1
        end = args_end(out, open_idx)
        args = split_args(out[open_idx + 1:end - 1])
        start = receiver_start(out, m.start())
        receiver = out[start:m.start()]
        method = m.group(1)
        replacement = None
        if method == 'getCapability' and args and args[0] in QUERIES:
            replacement = f'TransferHelper.{QUERIES[args[0]]}({", ".join([receiver] + args[1:])})'
        elif (method, args[0] if args else '') in REGISTER:
            helper = REGISTER[(method, args[0])]
            rest = args[1:]
            if method == 'registerItem':
                # (stack, context) -> handler becomes stack -> handler
                rest[0] = re.sub(r'^\((\w+),\s*\w+\)\s*->', r'\1 ->', rest[0])
            replacement = f'TransferHelper.{helper}({", ".join([receiver] + rest)})'
        if replacement is None:
            pos = end
            continue
        out = out[:start] + replacement + out[end:]
        pos = start + len(replacement)
    return out


def main():
    changed = 0
    for dirpath, _, files in os.walk(sys.argv[1]):
        for name in files:
            if not name.endswith('.java'):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding='utf-8', newline='') as f:
                text = f.read()
            new = rewrite(text)
            if new != text:
                if IMPORT not in new and 'package slimeknights.mantle.transfer;' not in new:
                    new = re.sub(r'^(import [^\n]*\n)', IMPORT + r'\n\1', new, count=1, flags=re.M)
                write(path, new)
                changed += 1
    print(f'changed {changed} files')


if __name__ == '__main__':
    main()
