"""File helpers for the port scripts. The repository lives in OneDrive, which briefly locks files while syncing."""
import time


def read(path):
    with open(path, encoding='utf-8', newline='') as f:
        return f.read()


def write(path, text):
    for _ in range(10):
        try:
            with open(path, 'w', encoding='utf-8', newline='') as f:
                f.write(text)
            return
        except OSError:
            time.sleep(0.5)
    raise OSError('could not write ' + path)


def edit(path, pairs, required=True):
    """Applies (old, new) replacements written with \n line endings to a file that may use \r\n."""
    text = read(path)
    crlf = '\r\n' in text
    norm = text.replace('\r\n', '\n')
    for old, new in pairs:
        if old not in norm:
            if required:
                raise ValueError(f'{path}: missing {old[:80]!r}')
            continue
        norm = norm.replace(old, new)
    out = norm.replace('\n', '\r\n') if crlf else norm
    if out != text:
        write(path, out)


def add_import(path, *imports):
    """Adds imports after the first import of a Java file, skipping existing ones"""
    import re
    text = read(path)
    nl = '\r\n' if '\r\n' in text else '\n'
    for imp in imports:
        line = 'import ' + imp + ';'
        if line not in text:
            text = re.sub(r'^(import [^\r\n]*\r?\n)', lambda m: line + nl + m.group(1), text, count=1, flags=re.M)
    write(path, text)
