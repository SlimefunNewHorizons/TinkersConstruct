"""Summarises javac errors from a Gradle log.

Usage: python scripts/port/errors.py <log> [mode] [filter]
  mode: symbols (default) | messages | files | packages | list
  filter: only errors whose file path contains this text
"""
import collections
import re
import sys

ERR = re.compile(r'^(.*?\.java):(\d+): error: (.*)$')


def parse(path, needle=None):
    errors = []
    seen = set()
    with open(path, encoding='utf-8', errors='replace') as f:
        lines = f.read().splitlines()
    i = 0
    while i < len(lines):
        m = ERR.match(lines[i])
        if m:
            file, line, msg = m.group(1), int(m.group(2)), m.group(3)
            file = file.replace('\\', '/').split('/src/main/java/')[-1]
            detail = []
            j = i + 1
            while j < len(lines) and not ERR.match(lines[j]) and j < i + 8:
                detail.append(lines[j])
                j += 1
            symbol = next((d.strip()[len('symbol:'):].strip() for d in detail if d.strip().startswith('symbol:')), '')
            location = next((d.strip()[len('location:'):].strip() for d in detail if d.strip().startswith('location:')), '')
            code = detail[0].strip() if detail else ''
            key = (file, line, msg, symbol)
            if (needle is None or needle in file) and key not in seen:
                seen.add(key)
                errors.append((file, line, msg, symbol, location, code))
            i = j
        else:
            i += 1
    return errors


def main():
    errors = parse(sys.argv[1], sys.argv[3] if len(sys.argv) > 3 else None)
    mode = sys.argv[2] if len(sys.argv) > 2 else 'symbols'
    print(f'{len(errors)} errors in {len({e[0] for e in errors})} files')
    if mode == 'symbols':
        c = collections.Counter((e[2] if not e[3] else e[2] + ' | ' + e[3]) for e in errors)
    elif mode == 'messages':
        c = collections.Counter(re.sub(r'\b[\w.$<>?,\[\] ]+\b', lambda m: m.group(0), e[2]) for e in errors)
    elif mode == 'files':
        c = collections.Counter(e[0] for e in errors)
    elif mode == 'packages':
        c = collections.Counter('/'.join(e[0].split('/')[:4]) for e in errors)
    else:
        for e in errors:
            print(f'{e[0]}:{e[1]}: {e[2]} {("| " + e[3]) if e[3] else ""}\n    {e[5][:160]}')
        return
    for k, v in c.most_common(int(sys.argv[4]) if len(sys.argv) > 4 else 80):
        print(f'{v:6} {k}')


if __name__ == '__main__':
    main()
