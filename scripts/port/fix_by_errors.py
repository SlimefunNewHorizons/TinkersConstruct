"""Applies mechanical 1.21.1 -> 1.21.11 rewrites to the exact lines javac reported.

Usage: python scripts/port/fix_by_errors.py <gradle-log>

Each rule matches an error message (and optional symbol) and rewrites only the
reported source line, so a rename never touches a call on an unrelated type.
"""
import collections
import re
import sys

sys.path.insert(0, __file__.rsplit('scripts', 1)[0] + 'scripts/port')
import errors  # noqa: E402

OPT_DEFAULTS = {
    'Integer': ('getInt', 'getIntOr', '0'),
    'Long': ('getLong', 'getLongOr', '0L'),
    'Float': ('getFloat', 'getFloatOr', '0f'),
    'Double': ('getDouble', 'getDoubleOr', '0d'),
    'Byte': ('getByte', 'getByteOr', '(byte) 0'),
    'Short': ('getShort', 'getShortOr', '(short) 0'),
    'Boolean': ('getBoolean', 'getBooleanOr', 'false'),
    'String': ('getString', 'getStringOr', '""'),
}

# Tag.TAG_* constant -> getter used to test presence of that type
CONTAINS_TYPES = {
    'TAG_COMPOUND': 'getCompound', 'TAG_LIST': 'getList', 'TAG_STRING': 'getString',
    'TAG_INT': 'getInt', 'TAG_LONG': 'getLong', 'TAG_FLOAT': 'getFloat', 'TAG_DOUBLE': 'getDouble',
    'TAG_BYTE': 'getByte', 'TAG_SHORT': 'getShort', 'TAG_ANY_NUMERIC': 'getDouble',
    'TAG_INT_ARRAY': 'getIntArray', 'TAG_BYTE_ARRAY': 'getByteArray', 'TAG_LONG_ARRAY': 'getLongArray',
    '10': 'getCompound', '9': 'getList', '8': 'getString', '3': 'getInt', '99': 'getDouble', '1': 'getByte',
}


def args_end(line, start):
    """Index just past the closing parenthesis matching the one at start."""
    depth = 0
    i = start
    in_str = None
    while i < len(line):
        ch = line[i]
        if in_str:
            if ch == '\\':
                i += 1
            elif ch == in_str:
                in_str = None
        elif ch in '"\'':
            in_str = ch
        elif ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return -1


def split_args(text):
    """Splits a comma separated argument list at depth 0."""
    out, depth, cur, in_str = [], 0, '', None
    for i, ch in enumerate(text):
        if in_str:
            cur += ch
            if ch == in_str and text[i - 1] != '\\':
                in_str = None
            continue
        if ch in '"\'':
            in_str = ch
        elif ch in '([{<':
            depth += 1
        elif ch in ')]}>':
            depth -= 1
        elif ch == ',' and depth == 0:
            out.append(cur)
            cur = ''
            continue
        cur += ch
    out.append(cur)
    return [a.strip() for a in out]


def rewrite_calls(line, method, fn):
    """Calls fn(args) -> replacement text (or None) for each .method( call on the line."""
    result = ''
    pos = 0
    pattern = re.compile(r'\.' + re.escape(method) + r'\s*\(')
    while True:
        m = pattern.search(line, pos)
        if not m:
            break
        open_idx = m.end() - 1
        end = args_end(line, open_idx)
        if end < 0:
            break
        args = split_args(line[open_idx + 1:end - 1]) if end - 1 > open_idx + 1 else []
        rep = fn(args)
        if rep is None:
            result += line[pos:end]
        else:
            result += line[pos:m.start()] + rep
        pos = end
    return result + line[pos:]


def fix_optional(kind):
    get, get_or, default = OPT_DEFAULTS[kind]

    def apply(line):
        return rewrite_calls(line, get, lambda a: f'.{get_or}({a[0]}, {default})' if len(a) == 1 else None)
    return apply


def fix_contains(line):
    def rep(a):
        if len(a) != 2:
            return None
        t = a[1].split('.')[-1]
        getter = CONTAINS_TYPES.get(t)
        return f'.{getter}({a[0]}).isPresent()' if getter else None
    return rewrite_calls(line, 'contains', rep)


def fix_get_list(line):
    return rewrite_calls(line, 'getList', lambda a: f'.getListOrEmpty({a[0]})' if len(a) == 2 else None)


def fix_get_compound(line):
    return rewrite_calls(line, 'getCompound', lambda a: f'.getCompoundOrEmpty({a[0]})' if len(a) == 1 else None)


def sub(pattern, repl):
    return lambda line: re.sub(pattern, repl, line)


RULES = [
    (r'isClientSide has private access', None, sub(r'\.isClientSide\b(?!\s*\()', '.isClientSide()')),
    (r'Optional<CompoundTag> cannot be converted to CompoundTag', None, fix_get_compound),
    (r'method contains in class CompoundTag cannot be applied', None, fix_contains),
    (r'method getList in class CompoundTag cannot be applied', None, fix_get_list),
    (r'cannot find symbol', r'^method getAllKeys\(\)', sub(r'\.getAllKeys\(\)', '.keySet()')),
    (r'cannot find symbol', r'^method readResourceLocation\(\)', sub(r'\.readResourceLocation\(\)', '.readIdentifier()')),
    (r'cannot find symbol', r'^method writeResourceLocation\(', sub(r'\.writeResourceLocation\(', '.writeIdentifier(')),
    (r'cannot find symbol', r'^method noCollission\(\)', sub(r'\.noCollission\(\)', '.noCollision()')),
    (r'cannot find symbol', r'^method getCommandSenderWorld\(\)', sub(r'\.getCommandSenderWorld\(\)', '.level()')),
] + [
    (rf'Optional<{k}> cannot be converted to', None, fix_optional(k)) for k in OPT_DEFAULTS
]


def main():
    errs = errors.parse(sys.argv[1])
    edits = collections.defaultdict(set)
    for file, line, msg, symbol, _, _ in errs:
        for i, (msg_re, sym_re, _) in enumerate(RULES):
            if re.search(msg_re, msg) and (sym_re is None or re.search(sym_re, symbol)):
                edits[file].add((line, i))
    changed = collections.Counter()
    for file, items in edits.items():
        path = 'src/main/java/' + file
        with open(path, encoding='utf-8', newline='') as f:
            lines = f.read().split('\n')
        for line_no, rule in sorted(items):
            old = lines[line_no - 1]
            new = RULES[rule][2](old)
            if new != old:
                lines[line_no - 1] = new
                changed[RULES[rule][0]] += 1
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write('\n'.join(lines))
    for k, v in changed.most_common():
        print(f'{v:5} {k}')


if __name__ == '__main__':
    main()
