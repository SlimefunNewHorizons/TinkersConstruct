"""Rewrites Minecraft/NeoForge imports whose class moved package between versions.

Usage: python scripts/port/fix_imports.py <class-index.txt> <src-root>...

class-index.txt lists one fully qualified class name per line for the target
Minecraft + NeoForge version (nested classes as Outer$Inner). An import that no
longer resolves is pointed at the single class with the same simple name, or the
candidate sharing the longest package prefix when several exist.
"""
import os
import re
import sys

# classes renamed between 1.21.1 and 1.21.11, keyed by old simple name
RENAMED = {
    'ResourceLocation': 'Identifier',
    'ResourceLocationException': 'IdentifierException',
    'ResourceLocationArgument': 'IdentifierArgument',
    'LavaSlimeModel': 'MagmaCubeModel',
    'TropicalFishModelA': 'TropicalFishSmallModel',
    'TropicalFishModelB': 'TropicalFishLargeModel',
}
PREFIXES = ('net.minecraft.', 'net.neoforged.neoforge.', 'com.mojang.blaze3d.', 'com.mojang.math.', 'com.mojang.realmsclient.')
IMPORT = re.compile(r'^import (static )?([\w.]+?)(\.\*)?;', re.M)


def load_index(path):
    known = set()
    by_simple = {}
    with open(path, encoding='utf-8') as f:
        for line in f:
            name = line.strip().replace('$', '.')
            if not name:
                continue
            known.add(name)
            by_simple.setdefault(name.rsplit('.', 1)[-1], []).append(name)
    return known, by_simple


def common_prefix(a, b):
    n = 0
    for x, y in zip(a.split('.'), b.split('.')):
        if x != y:
            break
        n += 1
    return n


def resolve(old, known, by_simple):
    """Returns the new name for a missing class, or None if unknown."""
    if old in known:
        return old
    # nested import: resolve the outermost missing class and keep the nested path
    parts = old.split('.')
    for i in range(len(parts) - 1, 0, -1):
        outer = '.'.join(parts[:i + 1])
        if parts[i][:1].isupper() and (i == 0 or not parts[i - 1][:1].isupper()):
            simple = RENAMED.get(parts[i], parts[i])
            cands = by_simple.get(simple, [])
            cands = [c for c in cands if c.startswith(PREFIXES)]
            if not cands:
                return None
            scores = sorted((common_prefix(c, outer) for c in cands), reverse=True)
            if len(scores) > 1 and scores[0] == scores[1]:
                return None  # ambiguous, leave for a manual fix
            best = max(cands, key=lambda c: common_prefix(c, outer))
            rest = parts[i + 1:]
            return '.'.join([best] + rest)
    return None


def fix_file(path, known, by_simple, report):
    with open(path, encoding='utf-8', newline='') as f:
        text = f.read()
    renames = {}

    def repl(m):
        static, name, star = m.group(1) or '', m.group(2), m.group(3) or ''
        if not name.startswith(PREFIXES):
            return m.group(0)
        target = name
        if static and not star:
            owner, member = name.rsplit('.', 1)
            new_owner = resolve(owner, known, by_simple)
            if new_owner is None:
                report.append(f'{path}: unresolved {name}')
                return m.group(0)
            target = new_owner + '.' + member
        else:
            new = resolve(name, known, by_simple)
            if new is None:
                report.append(f'{path}: unresolved {name}')
                return m.group(0)
            target = new
        if target != name:
            old_simple = name.rsplit('.', 1)[-1]
            new_simple = target.rsplit('.', 1)[-1]
            if old_simple != new_simple and not static:
                renames[old_simple] = new_simple
        return f'import {static}{target}{star};'

    new_text = IMPORT.sub(repl, text)
    for old, new in renames.items():
        new_text = re.sub(r'(?<![\w.])' + re.escape(old) + r'\b', new, new_text)
    if new_text != text:
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(new_text)
        return True
    return False


def main():
    known, by_simple = load_index(sys.argv[1])
    report = []
    changed = 0
    for root in sys.argv[2:]:
        for dirpath, _, files in os.walk(root):
            for name in files:
                if name.endswith('.java') and fix_file(os.path.join(dirpath, name), known, by_simple, report):
                    changed += 1
    print(f'changed {changed} files')
    unresolved = sorted({r.split(': unresolved ')[1] for r in report})
    print(f'{len(unresolved)} unresolved imports')
    for r in unresolved:
        print('  ' + r)


if __name__ == '__main__':
    main()
