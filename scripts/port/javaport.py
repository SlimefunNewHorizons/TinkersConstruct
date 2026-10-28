"""Helpers to edit Java sources in the port scripts"""
import re

from portio import add_import, read, write


def remove_method(text, signature):
  """Removes the method starting with the given signature line, along with its annotations"""
  start = text.index(signature)
  # include annotations and javadoc directly above
  line_start = text.rindex('\n', 0, start) + 1
  while True:
    prev_end = line_start - 1
    prev_start = text.rindex('\n', 0, prev_end) + 1
    prev = text[prev_start:prev_end].strip()
    if prev.startswith('@') or prev.startswith('/**') or prev.startswith('*') or prev.startswith('*/'):
      line_start = prev_start
    else:
      break
  depth = 0
  i = text.index('{', start)
  while True:
    if text[i] == '{':
      depth += 1
    elif text[i] == '}':
      depth -= 1
      if depth == 0:
        break
    i += 1
  end = text.index('\n', i) + 1
  # drop one blank line after
  if text[end:end + 1] == '\n':
    end += 1
  return text[:line_start] + text[end:]


def drop_lines(text, *needles):
  for needle in needles:
    text = re.sub(r'^[^\n]*' + re.escape(needle) + r'[^\n]*\n', '', text, flags=re.M)
  return text


def port(path, methods=(), lines=(), pairs=(), imports=()):
  text = read(path)
  nl = '\r\n' if '\r\n' in text else '\n'
  text = text.replace('\r\n', '\n')
  for method in methods:
    text = remove_method(text, method)
  text = drop_lines(text, *lines)
  for old, new in pairs:
    assert old in text, (path, old)
    text = text.replace(old, new)
  write(path, text.replace('\n', nl))
  if imports:
    add_import(path, *imports)
