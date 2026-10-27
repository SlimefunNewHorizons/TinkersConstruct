"""Makes blocks with appendHoverText implement Mantle's IBlockTooltip (blocks lost the method in 1.21.5). Usage: python scripts/port/block_tooltips.py <file>..."""
import re
import sys

from portio import read, write

IMPORT = 'import slimeknights.mantle.block.IBlockTooltip;'

for path in sys.argv[1:]:
    s = read(path)
    o = s
    cls = re.search(r'public (?:abstract )?class \w+(?:<[^{]*?>)? extends [\w.<>]+(?: implements ([^{]+?))?\s*\{', s)
    if cls and 'IBlockTooltip' not in s[cls.start():cls.end()]:
        if cls.group(1):
            s = s[:cls.end(1)] + ', IBlockTooltip' + s[cls.end(1):]
        else:
            brace = cls.end() - 1
            while s[brace - 1] == ' ':
                brace -= 1
            s = s[:brace] + ' implements IBlockTooltip ' + s[cls.end() - 1:]
    # Block has no appendHoverText to call anymore
    s = re.sub(r'\r?\n\s*super\.appendHoverText\([^;]*\);', '', s)
    if s != o:
        if IMPORT not in s and 'package slimeknights.mantle.block;' not in s:
            s = re.sub(r'^(import [^\n]*\n)', IMPORT + '\n' + r'\1', s, count=1, flags=re.M)
        write(path, s)
        print('updated', path)
