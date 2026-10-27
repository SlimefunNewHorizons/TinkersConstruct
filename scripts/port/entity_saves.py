"""1.21.6: Entity#addAdditionalSaveData/readAdditionalSaveData take ValueOutput/ValueInput.

Bridges the old CompoundTag bodies through ValueIOHelper, keeping the saved format.
Usage: python scripts/port/entity_saves.py <file>...
"""
import re
import sys

from portio import read, write

SIG = re.compile(r'(public|protected) void (addAdditionalSaveData|readAdditionalSaveData)\(CompoundTag (\w+)\)\s*\{')
IMPORTS = ['net.minecraft.world.level.storage.ValueInput', 'net.minecraft.world.level.storage.ValueOutput', 'slimeknights.mantle.util.ValueIOHelper']


def body_end(text, open_idx):
  depth = 0
  for i in range(open_idx, len(text)):
    if text[i] == '{':
      depth += 1
    elif text[i] == '}':
      depth -= 1
      if depth == 0:
        return i
  raise ValueError('unbalanced braces')


def convert(text):
  nl = '\r\n' if '\r\n' in text else '\n'
  pos = 0
  while True:
    m = SIG.search(text, pos)
    if not m:
      return text
    access, method, tag = m.groups()
    open_idx = m.end() - 1
    close_idx = body_end(text, open_idx)
    body = text[open_idx + 1:close_idx].strip('\r\n')
    if method == 'addAdditionalSaveData':
      body = body.replace(f'super.addAdditionalSaveData({tag});', 'super.addAdditionalSaveData(output);')
      body = body.rstrip()
      inner = nl.join(('  ' + line) if line.strip() else line for line in body.split(nl))
      new = (f'{access} void addAdditionalSaveData(ValueOutput output) {{{nl}'
             f'    CompoundTag {tag} = new CompoundTag();{nl}'
             f'    try {{{nl}{inner}{nl}'
             f'    }} finally {{{nl}'
             f'      ValueIOHelper.write(output, {tag});{nl}'
             f'    }}{nl}  }}')
    else:
      body = body.replace(f'super.readAdditionalSaveData({tag});', 'super.readAdditionalSaveData(input);')
      body = body.rstrip()
      new = (f'{access} void readAdditionalSaveData(ValueInput input) {{{nl}'
             f'    CompoundTag {tag} = ValueIOHelper.read(input);{nl}{body}{nl}  }}')
    text = text[:m.start()] + new + text[close_idx + 1:]
    pos = m.start() + len(new)


for path in sys.argv[1:]:
  text = read(path)
  new = convert(text)
  if new != text:
    nl = '\r\n' if '\r\n' in new else '\n'
    for imp in IMPORTS:
      line = 'import ' + imp + ';'
      if line not in new:
        new = re.sub(r'^(import [^\n]*\n)', line + nl + r'\1', new, count=1, flags=re.M)
    write(path, new)
    print('updated', path)
