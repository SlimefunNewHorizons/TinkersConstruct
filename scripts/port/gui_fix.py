"""Converts 1.21.1 GUI code to the 1.21.11 GuiGraphics and input APIs.

- blit/blitSprite without a render pipeline get RenderPipelines.GUI_TEXTURED and the 1.21.11 argument order
- PoseStack variables from graphics.pose() become Matrix3x2fStack (push/pop, 2D translate and scale, Z rotation)
- renderTooltip(font, list, Optional, x, y) becomes setTooltipForNextFrame
- mouse and key handlers take MouseButtonEvent/KeyEvent, the old parameters become locals

Color state (setColor, setShaderColor) is not converted, it has to be passed to the blit calls by hand.
Usage: python scripts/port/gui_fix.py <file or folder>...
"""
import os
import re
import sys

from portio import read, write

PIPELINE = 'RenderPipelines.GUI_TEXTURED'


def find_call(text, start):
  """Returns the index after the closing parenthesis of the call whose open parenthesis is at start"""
  depth = 0
  i = start
  in_str = None
  while i < len(text):
    c = text[i]
    if in_str:
      if c == '\\':
        i += 2
        continue
      if c == in_str:
        in_str = None
    elif c in '"\'':
      in_str = c
    elif c == '(':
      depth += 1
    elif c == ')':
      depth -= 1
      if depth == 0:
        return i + 1
    i += 1
  raise ValueError('unbalanced call at ' + str(start))


def split_args(args):
  """Splits top level arguments"""
  out, depth, cur, in_str = [], 0, '', None
  for c in args:
    if in_str:
      cur += c
      if c == in_str:
        in_str = None
      continue
    if c in '"\'':
      in_str = c
    elif c in '([{<':
      depth += 1
    elif c in ')]}>':
      depth -= 1
    if c == ',' and depth == 0:
      out.append(cur.strip())
      cur = ''
    else:
      cur += c
  if cur.strip():
    out.append(cur.strip())
  return out


def rewrite_calls(text, name, fn):
  """Rewrites each call to .name( using fn(args) -> new args or None to keep"""
  pattern = re.compile(r'\.' + name + r'\(')
  pos = 0
  while True:
    m = pattern.search(text, pos)
    if not m:
      return text
    open_paren = m.end() - 1
    end = find_call(text, open_paren)
    args = split_args(text[open_paren + 1:end - 1])
    new = fn(args)
    if new is None:
      pos = end
    else:
      replacement = '.' + name + '(' + ', '.join(new) + ')'
      text = text[:m.start()] + replacement + text[end:]
      pos = m.start() + len(replacement)


def is_pipeline(arg):
  return 'RenderPipeline' in arg or 'PIPELINE' in arg.upper()


def blit(args):
  if not args or is_pipeline(args[0]):
    return None
  n = len(args)
  if n == 6 and 'sprite' in args[5].lower():  # (x, y, z, w, h, sprite)
    return None  # handled as blitSprite below
  if n == 7:  # (tex, x, y, u, v, w, h), 256 texture
    return [PIPELINE, args[0], args[1], args[2], args[3], args[4], args[5], args[6], '256', '256']
  if n == 8:  # (tex, x, y, z, u, v, w, h), 256 texture
    return [PIPELINE, args[0], args[1], args[2], args[4], args[5], args[6], args[7], '256', '256']
  if n == 9:  # (tex, x, y, u, v, w, h, texW, texH)
    return [PIPELINE] + args
  if n == 10:  # (tex, x, y, z, u, v, w, h, texW, texH)
    return [PIPELINE, args[0], args[1], args[2]] + args[4:]
  if n == 11:  # (tex, x, y, w, h, u, v, uW, vH, texW, texH)
    return [PIPELINE, args[0], args[1], args[2], args[5], args[6], args[3], args[4], args[7], args[8], args[9], args[10]]
  return None


def blit_sprite(args):
  if not args or is_pipeline(args[0]):
    return None
  return [PIPELINE] + args


def tooltip(text):
  text = re.sub(r'\.renderTooltip\(([^;]*?), Optional\.empty\(\), ', r'.setTooltipForNextFrame(\1, Optional.empty(), ', text)
  text = re.sub(r'\.renderComponentTooltip\(', '.setComponentTooltipForNextFrame(', text)
  return text


def float_arg(arg):
  if re.fullmatch(r'-?\d+(\.\d+)?[fF]?', arg) or arg.endswith('f') or arg.endswith('F'):
    return arg.rstrip('dD')
  return '(float) (' + arg + ')' if not re.fullmatch(r'[\w.]+', arg) else '(float) ' + arg


def pose(text):
  """Converts PoseStack variables taken from graphics.pose()"""
  names = set()
  for m in re.finditer(r'PoseStack (\w+) = (\w+)\.pose\(\);', text):
    names.add(m.group(1))
  text = re.sub(r'PoseStack (\w+) = (\w+)\.pose\(\);', r'Matrix3x2fStack \1 = \2.pose();', text)
  receivers = [re.escape(n) for n in names] + [r'\w+\.pose\(\)']
  recv = '(' + '|'.join(receivers) + ')'
  text = re.sub(recv + r'\.pushPose\(\)', r'\1.pushMatrix()', text)
  text = re.sub(recv + r'\.popPose\(\)', r'\1.popMatrix()', text)

  def two(name, keep_z=False):
    def fn(args):
      if len(args) != 3:
        return None
      return [float_arg(args[0]), float_arg(args[1])]
    return fn

  for receiver in list(names) + ['pose()']:
    for method in ['translate', 'scale']:
      pattern = re.compile(re.escape(receiver) + r'\.' + method + r'\(')
      pos = 0
      while True:
        m = pattern.search(text, pos)
        if not m:
          break
        open_paren = m.end() - 1
        end = find_call(text, open_paren)
        args = split_args(text[open_paren + 1:end - 1])
        if len(args) == 3:
          new = receiver + '.' + method + '(' + float_arg(args[0]) + ', ' + float_arg(args[1]) + ')'
          text = text[:m.start()] + new + text[end:]
          pos = m.start() + len(new)
        else:
          pos = end
    text = re.sub(re.escape(receiver) + r'\.mulPose\(Axis\.ZP\.rotationDegrees\(([^;]*?)\)\);', receiver + r'.rotate((float) Math.toRadians(\1));', text)
  if names or '.pose().pushMatrix()' in text:
    if 'import org.joml.Matrix3x2fStack;' not in text:
      text = add_import(text, 'org.joml.Matrix3x2fStack')
  return text


def add_import(text, imp):
  line = 'import ' + imp + ';'
  if line in text:
    return text
  return re.sub(r'^(import [^\n]*\n)', line + '\n' + r'\1', text, count=1, flags=re.M)


INPUT = [
  # (old signature regex, new signature, locals)
  (r'public boolean mouseClicked\(double (\w+), double (\w+), int (\w+)\) \{',
   'public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {\n    double {0} = event.x();\n    double {1} = event.y();\n    int {2} = event.button();'),
  (r'public boolean mouseReleased\(double (\w+), double (\w+), int (\w+)\) \{',
   'public boolean mouseReleased(MouseButtonEvent event) {\n    double {0} = event.x();\n    double {1} = event.y();\n    int {2} = event.button();'),
  (r'public boolean mouseDragged\(double (\w+), double (\w+), int (\w+), double (\w+), double (\w+)\) \{',
   'public boolean mouseDragged(MouseButtonEvent event, double {3}, double {4}) {\n    double {0} = event.x();\n    double {1} = event.y();\n    int {2} = event.button();'),
  (r'public boolean keyPressed\(int (\w+), int (\w+), int (\w+)\) \{',
   'public boolean keyPressed(KeyEvent event) {\n    int {0} = event.key();\n    int {1} = event.scancode();\n    int {2} = event.modifiers();'),
  (r'public boolean keyReleased\(int (\w+), int (\w+), int (\w+)\) \{',
   'public boolean keyReleased(KeyEvent event) {\n    int {0} = event.key();\n    int {1} = event.scancode();\n    int {2} = event.modifiers();'),
  (r'public boolean charTyped\(char (\w+), int (\w+)\) \{',
   'public boolean charTyped(CharacterEvent event) {\n    char {0} = (char) event.codepoint();\n    int {1} = event.modifiers();'),
]
SUPER = [
  (r'super\.mouseClicked\(([^,;]+), ([^,;]+), ([^,;]+)\)', 'super.mouseClicked(new MouseButtonEvent({0}, {1}, new MouseButtonInfo({2}, event.modifiers())), doubleClick)'),
  (r'super\.mouseReleased\(([^,;]+), ([^,;]+), ([^,;]+)\)', 'super.mouseReleased(new MouseButtonEvent({0}, {1}, new MouseButtonInfo({2}, event.modifiers())))'),
  (r'super\.mouseDragged\(([^,;]+), ([^,;]+), ([^,;]+), ([^,;]+), ([^,;]+)\)', 'super.mouseDragged(new MouseButtonEvent({0}, {1}, new MouseButtonInfo({2}, event.modifiers())), {3}, {4})'),
  (r'super\.keyPressed\(([^,;]+), ([^,;]+), ([^,;]+)\)', 'super.keyPressed(new KeyEvent({0}, {1}, {2}))'),
  (r'super\.keyReleased\(([^,;]+), ([^,;]+), ([^,;]+)\)', 'super.keyReleased(new KeyEvent({0}, {1}, {2}))'),
  (r'super\.charTyped\(([^,;]+), ([^,;]+)\)', 'super.charTyped(new CharacterEvent({0}, {1}))'),
]


def fill(template, groups):
  for i, group in enumerate(groups):
    template = template.replace('{' + str(i) + '}', group)
  return template


def body_end(text, open_brace):
  """Index after the brace closing the block opened at open_brace"""
  depth = 0
  for i in range(open_brace, len(text)):
    if text[i] == '{':
      depth += 1
    elif text[i] == '}':
      depth -= 1
      if depth == 0:
        return i + 1
  return len(text)


def inputs(text):
  """Converts handler signatures, and super calls inside the converted handlers only"""
  changed = False
  for pattern, template in INPUT:
    pos = 0
    while True:
      m = re.compile(pattern).search(text, pos)
      if not m:
        break
      signature = fill(template, m.groups())
      open_brace = m.start() + signature.index('{')
      text = text[:m.start()] + signature + text[m.end():]
      end = body_end(text, open_brace)
      body = text[open_brace:end]
      for super_pattern, super_template in SUPER:
        body = re.sub(super_pattern, lambda sm, t=super_template: fill(t, sm.groups()), body)
      text = text[:open_brace] + body + text[end:]
      pos = open_brace + len(body)
      changed = True
  if changed:
    for imp in ['net.minecraft.client.input.MouseButtonEvent', 'net.minecraft.client.input.MouseButtonInfo', 'net.minecraft.client.input.KeyEvent', 'net.minecraft.client.input.CharacterEvent']:
      if imp.rsplit('.', 1)[1] + '(' in text or imp.rsplit('.', 1)[1] + ' ' in text:
        text = add_import(text, imp)
  return text


def convert(text):
  nl = '\r\n' if '\r\n' in text else '\n'
  text = text.replace('\r\n', '\n')
  orig = text
  text = rewrite_calls(text, 'blit', blit)
  text = re.sub(r'\.blit\(([^;]*?), ([^,;]*?), ([^,;]*?), ([^,;]*?), ([^,;]*?), (\w*[sS]prite\w*)\)', lambda m: '.blitSprite(' + PIPELINE + ', ' + m.group(6) + ', ' + m.group(1) + ', ' + m.group(2) + ', ' + m.group(4) + ', ' + m.group(5) + ')', text)
  text = rewrite_calls(text, 'blitSprite', blit_sprite)
  text = tooltip(text)
  text = pose(text)
  text = inputs(text)
  if PIPELINE in text and PIPELINE not in orig:
    text = add_import(text, 'net.minecraft.client.renderer.RenderPipelines')
  return text.replace('\n', nl)


def main():
  for arg in sys.argv[1:]:
    paths = [arg] if os.path.isfile(arg) else [os.path.join(d, f) for d, _, fs in os.walk(arg) for f in fs if f.endswith('.java')]
    for path in paths:
      text = read(path)
      new = convert(text)
      if new != text:
        write(path, new)
        print('converted', path)


if __name__ == '__main__':
  main()
