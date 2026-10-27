"""Removes overrides of methods that no longer exist, with their annotations and javadoc.

Usage: python scripts/port/remove_methods.py <file> <method>...
Only removes methods whose declaration line starts with 'public' or 'protected' and that have a body.
"""
import re
import sys

from portio import read, write


def remove(text, name):
  pattern = re.compile(r'(\r?\n)((?:[ \t]*(?:/\*\*.*?\*/|@\w+(?:\([^)]*\))?)[ \t]*\r?\n)*)[ \t]*(?:public|protected)[^;{=]*?\b' + re.escape(name) + r'\s*\([^;{]*?\)\s*(?:throws [\w., ]+)?\{', re.S)
  m = pattern.search(text)
  if not m:
    return text, False
  depth = 0
  i = m.end() - 1
  while True:
    if text[i] == '{':
      depth += 1
    elif text[i] == '}':
      depth -= 1
      if depth == 0:
        break
    i += 1
  end = i + 1
  # also drop the blank line after the method
  nl = m.group(1)
  if text.startswith(nl + nl, end) or text.startswith(nl, end) and text[end + len(nl):].startswith(nl):
    end += len(nl)
  return text[:m.start()] + text[end:], True


if __name__ == '__main__':
  path = sys.argv[1]
  text = read(path)
  for name in sys.argv[2:]:
    while True:
      text, found = remove(text, name)
      if not found:
        break
      print('removed', name)
  write(path, text)
