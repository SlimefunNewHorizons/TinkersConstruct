"""Removes single type imports whose simple name is not used anywhere else in the file.

Usage: python scripts/port/prune_imports.py <src-root>...

Unused imports of classes that no longer exist are compile errors after a port, removing every unused import is the simplest safe fix.
"""
import os
import re
import sys

from portio import read, write

IMPORT = re.compile(r'^import (?!static )([\w.]+)\.([A-Z]\w*);\r?\n', re.M)


def prune(text):
    body = IMPORT.sub('', text)
    # javadoc references count as uses
    def keep(m):
        simple = m.group(2)
        return m.group(0) if re.search(r'(?<![\w.])' + re.escape(simple) + r'\b', body) else ''
    return IMPORT.sub(keep, text)


def main():
    changed = 0
    for root in sys.argv[1:]:
        for dirpath, _, files in os.walk(root):
            for name in files:
                if name.endswith('.java'):
                    path = os.path.join(dirpath, name)
                    text = read(path)
                    new = prune(text)
                    if new != text:
                        write(path, new)
                        changed += 1
    print(f'pruned imports in {changed} files')


if __name__ == '__main__':
    main()
