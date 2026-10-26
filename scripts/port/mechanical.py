"""Global regex rewrites for 1.21.1 -> 1.21.11 API changes that are safe on any receiver.

Usage: python scripts/port/mechanical.py <src-root>...

Every rule must be idempotent: running the script twice changes nothing more.
"""
import os
import re
import sys

RULES = [
    # 1.21.2: InteractionResultHolder and ItemInteractionResult merged into InteractionResult
    (r'InteractionResultHolder\.(?:success|sidedSuccess)\(([^()]*(?:\([^()]*\))?[^()]*?)(?:,\s*[^()]*(?:\([^()]*\))?[^()]*)?\)',
     r'InteractionResult.SUCCESS.heldItemTransformedTo(\1)'),
    (r'InteractionResultHolder\.consume\(([^()]*(?:\([^()]*\))?[^()]*)\)', r'InteractionResult.CONSUME.heldItemTransformedTo(\1)'),
    (r'InteractionResultHolder\.pass\([^()]*(?:\([^()]*\))?[^()]*\)', 'InteractionResult.PASS'),
    (r'InteractionResultHolder\.fail\([^()]*(?:\([^()]*\))?[^()]*\)', 'InteractionResult.FAIL'),
    (r'InteractionResultHolder<ItemStack>', 'InteractionResult'),
    (r'ItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION', 'InteractionResult.TRY_WITH_EMPTY_HAND'),
    (r'ItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION', 'InteractionResult.PASS'),
    (r'ItemInteractionResult\.sidedSuccess\([^()]*(?:\([^()]*\))?[^()]*\)', 'InteractionResult.SUCCESS'),
    (r'InteractionResult\.sidedSuccess\([^()]*(?:\([^()]*\))?[^()]*\)', 'InteractionResult.SUCCESS'),
    (r'InteractionResult\.CONSUME_PARTIAL\b', 'InteractionResult.CONSUME'),
    (r'\bItemInteractionResult\b', 'InteractionResult'),
    (r'^import net\.minecraft\.world\.InteractionResultHolder;\r?\n', ''),
    # 1.21.2: UseAnim -> ItemUseAnimation
    (r'\bUseAnim\.SPEAR\b', 'ItemUseAnimation.TRIDENT'),
    (r'\bUseAnim\b', 'ItemUseAnimation'),
    (r'^import net\.minecraft\.world\.item\.ItemUseAnimation;\r?\n', ''),
    (r'^(package [\w.]+;\r?\n)', None),  # placeholder, handled in add_imports
]

# imports to add when a file uses the simple name but does not import it
NEEDED_IMPORTS = {
    'InteractionResult': 'net.minecraft.world.InteractionResult',
    'ItemUseAnimation': 'net.minecraft.world.item.ItemUseAnimation',
}


def add_imports(text):
    for simple, fqcn in NEEDED_IMPORTS.items():
        if re.search(r'\b' + simple + r'\b', text) and not re.search(r'^import ' + re.escape(fqcn) + ';', text, re.M):
            pkg = re.search(r'^package ([\w.]+);', text, re.M)
            if pkg and fqcn.rsplit('.', 1)[0] == pkg.group(1):
                continue
            m = re.search(r'^import [^\n]*\n', text, re.M) or re.search(r'^package [^\n]*\n', text, re.M)
            if m:
                nl = '\r\n' if '\r\n' in text else '\n'
                insert = f'import {fqcn};{nl}'
                text = text[:m.start()] + insert + text[m.start():] if m.group(0).startswith('import') else text[:m.end()] + nl + insert + text[m.end():]
    return text


def main():
    changed = 0
    for root in sys.argv[1:]:
        for dirpath, _, files in os.walk(root):
            for name in files:
                if not name.endswith('.java'):
                    continue
                path = os.path.join(dirpath, name)
                with open(path, encoding='utf-8', newline='') as f:
                    text = f.read()
                new = text
                for pattern, repl in RULES:
                    if repl is not None:
                        new = re.sub(pattern, repl, new, flags=re.M)
                if new != text:
                    new = add_imports(new)
                    with open(path, 'w', encoding='utf-8', newline='') as f:
                        f.write(new)
                    changed += 1
    print(f'changed {changed} files')


if __name__ == '__main__':
    main()
