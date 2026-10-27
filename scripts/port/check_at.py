"""Checks access transformer entries against decompiled 1.21.11 sources, suggesting moved classes.

Usage: python scripts/port/check_at.py <at.cfg> <decompiled-src-root> <class-index.txt>
"""
import os
import re
import sys

at, root, index = sys.argv[1:4]
# long path prefix, decompiled paths exceed MAX_PATH on Windows
if os.name == 'nt':
  root = '\\\\?\\' + os.path.abspath(root)
by_simple = {}
for line in open(index, encoding='utf-8'):
  name = line.strip()
  if name:
    by_simple.setdefault(name.rsplit('.', 1)[-1].split('$')[-1], []).append(name)

for n, line in enumerate(open(at, encoding='utf-8'), 1):
  line = line.split('#')[0].strip()
  if not line:
    continue
  parts = line.split()
  cls = parts[1]
  outer = cls.split('$')[0]
  path = os.path.join(root, *outer.split('.')) + '.java'
  if not os.path.exists(path):
    print(f'{n}: missing class {cls} -> {by_simple.get(outer.rsplit(".", 1)[-1], [])}')
    continue
  if len(parts) > 2:
    m = re.match(r'[\w<>$]+', parts[2])
    member = m.group(0) if m else '<init>'  # wildcard
    if member != '<init>' and not re.search(r'\b' + re.escape(member) + r'\b', open(path, encoding='utf-8').read()):
      print(f'{n}: missing member {cls} {parts[2]}')
