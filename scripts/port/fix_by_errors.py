"""Applies mechanical 1.21.1 -> 1.21.11 rewrites to the exact lines javac reported.

Usage: python scripts/port/fix_by_errors.py <gradle-log>

Each rule matches an error message, symbol and location, then rewrites only the
reported source line (using javac's caret column where it matters), so a rename
never touches a call on an unrelated type.
"""
import collections
import re
import sys
import time

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


def at_caret(old, new):
    """Replaces the occurrence of old nearest to the caret column."""
    def apply(line, err):
        caret = err[6]
        best = None
        for m in re.finditer(re.escape(old), line):
            dist = abs(m.start() - caret)
            if best is None or dist < best[0]:
                best = (dist, m)
        if best is None:
            return line
        m = best[1]
        return line[:m.start()] + new + line[m.end():]
    return apply


def method_before_caret(old, new):
    """Renames the method call whose opening parenthesis is at the caret, e.g. registry.get( -> registry.getValue("""
    def apply(line, err):
        caret = err[6]
        if caret < 0 or caret >= len(line) or line[caret] != '(':
            return line
        start = caret - len(old)
        if start >= 1 and line[start:caret] == old and line[start - 1] == '.':
            return line[:start] + new + line[caret:]
        return line
    return apply


def receiver_start(text, end):
    """Finds the start of the expression ending right before index end (exclusive), e.g. foo.bar(baz)."""
    i = end
    while i > 0:
        ch = text[i - 1]
        if ch == ')':
            depth = 0
            j = i - 1
            while j >= 0:
                if text[j] == ')':
                    depth += 1
                elif text[j] == '(':
                    depth -= 1
                    if depth == 0:
                        break
                j -= 1
            i = j
        elif ch.isalnum() or ch in '_.':
            i -= 1
        else:
            break
    return i


def wrap_receiver(call, helper):
    """Turns receiver.call into helper(receiver) for the occurrence nearest the caret, e.g. x.getItems() -> IngredientHelper.getItems(x)"""
    def apply(line, err):
        caret = err[6]
        best = None
        for m in re.finditer(re.escape(call), line):
            dist = abs(m.start() - caret)
            if best is None or dist < best[0]:
                best = (dist, m)
        if best is None:
            return line
        m = best[1]
        start = receiver_start(line, m.start())
        if start == m.start():
            return line
        return line[:start] + helper + '(' + line[start:m.start()] + ')' + line[m.end():]
    return apply


def call_with_receiver(method, fn):
    """Rewrites the receiver.method(args) call nearest the caret with fn(receiver, args) -> text"""
    def apply(line, err):
        caret = err[6]
        best = None
        for m in re.finditer(r'\.' + re.escape(method) + r'\s*\(', line):
            dist = abs(m.start() - caret)
            if best is None or dist < best[0]:
                best = (dist, m)
        if best is None:
            return line
        m = best[1]
        start = receiver_start(line, m.start())
        if start == m.start():
            return line
        end = args_end(line, m.end() - 1)
        if end < 0:
            return line
        args = split_args(line[m.end():end - 1]) if end - 1 > m.end() else []
        return line[:start] + fn(line[start:m.start()], args) + line[end:]
    return apply


def whole_line(fn):
    return lambda line, err: fn(line)


def fix_optional(kind):
    get, get_or, default = OPT_DEFAULTS[kind]
    return whole_line(lambda line: rewrite_calls(line, get, lambda a: f'.{get_or}({a[0]}, {default})' if len(a) == 1 else None))


def fix_contains(line):
    def rep(a):
        if len(a) != 2:
            return None
        t = a[1].split('.')[-1]
        getter = CONTAINS_TYPES.get(t)
        return f'.{getter}({a[0]}).isPresent()' if getter else None
    return rewrite_calls(line, 'contains', rep)


def sub(pattern, repl):
    return whole_line(lambda line: re.sub(pattern, repl, line))


def sound_value(line, err):
    """Appends .value() to the SoundEvents holder nearest the caret"""
    caret = err[6]
    best = None
    for m in re.finditer(r'SoundEvents\.\w+\b(?!\.value\(\))', line):
        dist = abs(m.start() - caret)
        if best is None or dist < best[0]:
            best = (dist, m)
    if best is None:
        return line
    m = best[1]
    return line[:m.end()] + '.value()' + line[m.end():]


# (message regex, symbol regex or None, location regex or None, fixer(line, err))
RULES = [
    (r'isClientSide has private access', None, None, sub(r'\.isClientSide\b(?!\s*\()', '.isClientSide()')),
    (r'Optional<CompoundTag> cannot be converted to CompoundTag', None, None,
     whole_line(lambda line: rewrite_calls(line, 'getCompound', lambda a: f'.getCompoundOrEmpty({a[0]})' if len(a) == 1 else None))),
    (r'method contains in class CompoundTag cannot be applied', None, None, whole_line(fix_contains)),
    (r'method getList in class CompoundTag cannot be applied', None, None,
     whole_line(lambda line: rewrite_calls(line, 'getList', lambda a: f'.getListOrEmpty({a[0]})' if len(a) == 2 else None))),
    (r'cannot find symbol', r'^method getAllKeys\(\)', None, sub(r'\.getAllKeys\(\)', '.keySet()')),
    (r'cannot find symbol', r'^method readResourceLocation\(\)', None, sub(r'\.readResourceLocation\(\)', '.readIdentifier()')),
    (r'cannot find symbol', r'^method writeResourceLocation\(', None, sub(r'\.writeResourceLocation\(', '.writeIdentifier(')),
    (r'cannot find symbol', r'^method noCollission\(\)', None, sub(r'\.noCollission\(\)', '.noCollision()')),
    (r'cannot find symbol', r'^method getCommandSenderWorld\(\)', None, sub(r'\.getCommandSenderWorld\(\)', '.level()')),
    # 1.21.11: ResourceKey#location -> identifier
    (r'cannot find symbol', r'^method location\(\)', r'ResourceKey', at_caret('.location()', '.identifier()')),
    # 1.21.2 registry API
    (r'Optional<Reference<.*>> cannot be converted to', None, None, method_before_caret('get', 'getValue')),
    (r'cannot find symbol', r'^method registryOrThrow\(', None, sub(r'\.registryOrThrow\(', '.lookupOrThrow(')),
    (r'cannot find symbol', r'^method getTag\(TagKey', r'Registry|HolderGetter|HolderLookup', at_caret('.getTag(', '.get(')),
    (r'cannot find symbol', r'^method getHolderOrThrow\(', None, at_caret('.getHolderOrThrow(', '.getOrThrow(')),
    (r'cannot find symbol', r'^method getTagNames\(\)', r'Registry', at_caret('.getTagNames()', '.listTagIds()')),
    (r'cannot find symbol', r'^method holders\(\)', r'Registry', at_caret('.holders()', '.listElements()')),
    (r'cannot find symbol', r'^method getHolder\(int\)', r'Registry', at_caret('.getHolder(', '.get(')),
    (r'cannot find symbol', r'^method getHolder\(ResourceKey', r'Registry', at_caret('.getHolder(', '.get(')),
    # assorted 1.21.2 - 1.21.11 renames
    (r'cannot find symbol', r'^method serverLevel\(\)', None, at_caret('.serverLevel()', '.level()')),
    (r'cannot find symbol', r'^method getDisplayName\(\)', r'FluidStack', at_caret('.getDisplayName()', '.getHoverName()')),
    (r'cannot find symbol', r'^method getParamOrNull\(', None, at_caret('.getParamOrNull(', '.getOptionalParameter(')),
    (r'cannot find symbol', r'^method getCraftingRemainingItem\(\)', r'ItemStack', at_caret('.getCraftingRemainingItem()', '.getCraftingRemainder()')),
    (r'cannot find symbol', r'^variable TAG_ANY_NUMERIC', None, whole_line(fix_contains)),
    (r'cannot find symbol', r'^method getGameRules\(\)', r'Level', call_with_receiver('getGameRules', lambda r, a: f'((net.minecraft.server.level.ServerLevel) {r}).getGameRules()')),
    (r'cannot find symbol', r'^method isFluidEqual\(FluidStack\)', None, call_with_receiver('isFluidEqual', lambda r, a: f'FluidStack.isSameFluidSameComponents({r}, {a[0]})')),
    (r'cannot find symbol', r'^method sendToServer\(', r'PacketDistributor', at_caret('PacketDistributor.sendToServer(', 'net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(')),
    # 1.21.2 ingredients
    (r'cannot find symbol', r'^method getItems\(\)', r'Ingredient', wrap_receiver('.getItems()', 'IngredientHelper.getItems')),
    (r'no suitable method found for of\(TagKey<Item>\)', None, None, at_caret('Ingredient.of(', 'IngredientHelper.of(')),
    # 1.21.2: sound events are holders, hurt is server only and returns void
    (r'Reference<SoundEvent> cannot be converted to SoundEvent', None, None, sound_value),
    (r'void cannot be converted to boolean', None, None, at_caret('.hurt(', '.hurtOrSimulate(')),
    # 1.21.5: CompoundTag#getString returns an optional
    (r'no suitable method found for \w+\(Optional<String>', None, None,
     whole_line(lambda line: rewrite_calls(line, 'getString', lambda a: f'.getStringOr({a[0]}, "")' if len(a) == 1 else None))),
    # removed stack and fluid NBT methods
    (r'cannot find symbol', r'^method parseOptional\(', r'class ItemStack', at_caret('ItemStack.parseOptional(', 'ValueIOHelper.parseStack(')),
    (r'cannot find symbol', r'^method parseOptional\(', r'class FluidStack', at_caret('FluidStack.parseOptional(', 'ValueIOHelper.parseFluid(')),
    (r'cannot find symbol', r'^method save\((Provider|RegistryAccess|Frozen)\)', r'of type (ItemStack|FluidStack)', call_with_receiver('save', lambda r, a: f'ValueIOHelper.save{"Fluid" if "luid" in r else "Stack"}({r}, {a[0]})')),
    (r'cannot find symbol', r'^method getMaxBuildHeight\(\)', None, call_with_receiver('getMaxBuildHeight', lambda r, a: f'({r}.getMaxY() + 1)')),
    (r'cannot find symbol', r'^method getAsString\(\)', r'Tag', call_with_receiver('getAsString', lambda r, a: f'{r}.asString().orElse("")')),
] + [
    (rf'Optional<{k}> cannot be converted to', None, None, fix_optional(k)) for k in OPT_DEFAULTS
]

# imports a rewrite may need, added when the helper name appears in the file
IMPORTS = {
    'IngredientHelper.': 'import slimeknights.mantle.recipe.helper.IngredientHelper;',
    'ValueIOHelper.': 'import slimeknights.mantle.util.ValueIOHelper;',
    'TransferHelper.': 'import slimeknights.mantle.transfer.TransferHelper;',
}


def write(path, text):
    for _ in range(5):
        try:
            with open(path, 'w', encoding='utf-8', newline='') as f:
                f.write(text)
            return
        except OSError:
            time.sleep(1)  # OneDrive may briefly lock files
    raise OSError('could not write ' + path)


def main():
    errs = errors.parse(sys.argv[1])
    edits = collections.defaultdict(list)
    for err in errs:
        file, line, msg, symbol, location = err[:5]
        for i, (msg_re, sym_re, loc_re, _) in enumerate(RULES):
            if re.search(msg_re, msg) and (sym_re is None or re.search(sym_re, symbol)) and (loc_re is None or re.search(loc_re, location)):
                edits[file].append((line, err[6], i, err))
                break
    changed = collections.Counter()
    for file, items in edits.items():
        path = 'src/main/java/' + file
        with open(path, encoding='utf-8', newline='') as f:
            lines = f.read().split('\n')
        # rightmost edits first so earlier carets stay valid
        for line_no, _, rule, err in sorted(items, key=lambda x: (x[0], -x[1])):
            old = lines[line_no - 1].rstrip('\r')
            cr = '\r' if lines[line_no - 1].endswith('\r') else ''
            new = RULES[rule][3](old, err)
            if new != old:
                lines[line_no - 1] = new + cr
                changed[RULES[rule][0] + ' ' + (RULES[rule][1] or '')] += 1
        write(path, '\n'.join(lines))
    for k, v in changed.most_common():
        print(f'{v:5} {k}')


if __name__ == '__main__':
    main()
