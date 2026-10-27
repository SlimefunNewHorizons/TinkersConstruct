"""Per-file error report with current source lines. Usage: python scripts/port/report.py <log> [exclude...]"""
import collections
import sys

sys.path.insert(0, __file__.rsplit('scripts', 1)[0] + 'scripts/port')
from errors import parse  # noqa: E402

exclude = sys.argv[2:]
by = collections.defaultdict(set)
for f, line, msg, sym, loc, code, caret in parse(sys.argv[1]):
  if any(e in f for e in exclude):
    continue
  by[f].add((line, msg, sym))
total = 0
for f in sorted(by):
  src = open('src/main/java/' + f, encoding='utf-8').read().splitlines()
  print('##', f)
  for line, msg, sym in sorted(by[f]):
    total += 1
    print(f'  {line}: {msg[:90]} {sym[:60]}\n      {src[line - 1].strip()[:200]}')
print('total', total)
