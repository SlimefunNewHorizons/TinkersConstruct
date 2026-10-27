"""Adapts Gson based SimpleJsonResourceReloadListener subclasses to the 1.21.2 codec based constructor.

Usage: python scripts/port/json_listeners.py <file>...

The listener keeps receiving raw JsonElements (ExtraCodecs.JSON), so apply() stays the same.
"""
import re
import sys

from portio import read, write

IMPORTS = ['import net.minecraft.util.ExtraCodecs;', 'import net.minecraft.resources.FileToIdConverter;', 'import com.mojang.serialization.JsonOps;']

for path in sys.argv[1:]:
    s = read(path)
    o = s
    if re.search(r'extends SimpleJsonResourceReloadListener \{', s):
        s = s.replace('extends SimpleJsonResourceReloadListener {', 'extends SimpleJsonResourceReloadListener<JsonElement> {')
        s = re.sub(r'super\(([\w.]+), (\w+)\);', r'super(ExtraCodecs.JSON, FileToIdConverter.json(\2));', s, count=1)
    s = re.sub(r'SimpleJsonResourceReloadListener\.scanDirectory\((\w+), ([\w.]+), [\w.]+, (\w+)\)',
               r'SimpleJsonResourceReloadListener.scanDirectory(\1, FileToIdConverter.json(\2), JsonOps.INSTANCE, ExtraCodecs.JSON, \3)', s)
    if s != o:
        for imp in IMPORTS:
            simple = imp.rsplit('.', 1)[1][:-1]
            if simple + '.' in s and imp not in s:
                s = re.sub(r'^(import [^\n]*\n)', imp + r'\n\1', s, count=1, flags=re.M)
        if 'JsonElement' in s and 'import com.google.gson.JsonElement;' not in s:
            s = re.sub(r'^(import [^\n]*\n)', 'import com.google.gson.JsonElement;\n' + r'\1', s, count=1, flags=re.M)
        write(path, s)
        print('updated', path)
