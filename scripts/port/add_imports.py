"""Adds missing imports for classes javac could not find, when the simple name resolves to one class.

Usage: python scripts/port/add_imports.py <gradle-log> <class-index.txt>

Candidates come from the Minecraft/NeoForge class index plus every top level class in src/main/java.
"""
import collections
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import errors  # noqa: E402
from portio import read, write  # noqa: E402

# names that resolve to several classes, pick the one the port means
PREFERRED = {
    'Identifier': 'net.minecraft.resources.Identifier',
    'Component': 'net.minecraft.network.chat.Component',
    'Item': 'net.minecraft.world.item.Item',
    'Block': 'net.minecraft.world.level.block.Block',
    'Level': 'net.minecraft.world.level.Level',
    'Codec': 'com.mojang.serialization.Codec',
    'MapCodec': 'com.mojang.serialization.MapCodec',
    'Nullable': 'javax.annotation.Nullable',
    'InteractionResult': 'net.minecraft.world.InteractionResult',
    'ItemUseAnimation': 'net.minecraft.world.item.ItemUseAnimation',
    'ServerLevel': 'net.minecraft.server.level.ServerLevel',
    'EquipmentSlot': 'net.minecraft.world.entity.EquipmentSlot',
    'TooltipDisplay': 'net.minecraft.world.item.component.TooltipDisplay',
    'Consumer': 'java.util.function.Consumer',
    'ValueInput': 'net.minecraft.world.level.storage.ValueInput',
    'ValueOutput': 'net.minecraft.world.level.storage.ValueOutput',
}


def project_classes():
    out = collections.defaultdict(list)
    for dirpath, _, files in os.walk('src/main/java'):
        for name in files:
            if name.endswith('.java') and name != 'package-info.java':
                rel = os.path.relpath(os.path.join(dirpath, name), 'src/main/java').replace('\\', '/')
                out[name[:-5]].append(rel[:-5].replace('/', '.'))
    return out


def main():
    errs = errors.parse(sys.argv[1])
    index = collections.defaultdict(list)
    with open(sys.argv[2], encoding='utf-8') as f:
        for line in f:
            name = line.strip()
            if name and '$' not in name:
                index[name.rsplit('.', 1)[-1]].append(name)
    for simple, names in project_classes().items():
        index[simple].extend(names)
    needed = collections.defaultdict(set)
    for file, line, msg, symbol, *_ in errs:
        m = re.match(r'^(class|variable) ([A-Z]\w*)$', symbol)
        if msg == 'cannot find symbol' and m:
            simple = m.group(2)
            fqcn = PREFERRED.get(simple)
            if fqcn is None:
                cands = index.get(simple, [])
                if len(cands) == 1:
                    fqcn = cands[0]
            if fqcn:
                needed[file].add(fqcn)
    count = 0
    for file, fqcns in needed.items():
        path = 'src/main/java/' + file
        text = read(path)
        pkg = re.search(r'^package ([\w.]+);', text, re.M)
        added = []
        for fqcn in sorted(fqcns):
            if pkg and fqcn.rsplit('.', 1)[0] == pkg.group(1):
                continue
            if re.search(r'^import ' + re.escape(fqcn) + ';', text, re.M):
                continue
            added.append(f'import {fqcn};')
        if added:
            nl = '\r\n' if '\r\n' in text else '\n'
            m = re.search(r'^import [^\n]*\n', text, re.M)
            if m:
                text = text[:m.start()] + nl.join(added) + nl + text[m.start():]
            else:
                m = re.search(r'^package [^\n]*\n', text, re.M)
                text = text[:m.end()] + nl + nl.join(added) + nl + text[m.end():]
            write(path, text)
            count += len(added)
    print(f'added {count} imports')


if __name__ == '__main__':
    main()
