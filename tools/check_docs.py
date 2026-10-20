#!/usr/bin/env python3
"""Check relative Markdown file links using only the Python standard library.

External URLs, anchors, inline code and fenced code are intentionally excluded.
This checks file existence, not remote URLs or heading anchors.
"""
from pathlib import Path
import re
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parent.parent
EXCLUDED = {'.git', '.gradle', '.idea', 'build', 'archiv'}
LINK = re.compile(r'!?\[[^\]\n]*\]\((<[^>]+>|[^)\s]+)(?:\s+"[^"]*")?\)')


def main():
    failures = []
    count = 0
    documents = sorted(p for p in ROOT.rglob('*.md')
                       if not EXCLUDED.intersection(p.relative_to(ROOT).parts))
    for document in documents:
        fenced = False
        for number, line in enumerate(document.read_text(encoding='utf-8').splitlines(), 1):
            if line.lstrip().startswith(('```', '~~~')):
                fenced = not fenced
                continue
            if fenced:
                continue
            line = re.sub(r'`[^`]*`', '', line)
            for match in LINK.finditer(line):
                target = match.group(1).strip('<>')
                parts = urlsplit(target)
                if parts.scheme or parts.netloc or not parts.path:
                    continue
                count += 1
                path = (document.parent / unquote(parts.path)).resolve()
                if not path.is_relative_to(ROOT) or not path.exists():
                    failures.append(f'{document.relative_to(ROOT)}:{number}: {target}')
    if failures:
        print('Invalid relative file links:', *failures, sep='\n', file=sys.stderr)
        return 1
    print(f'OK: {len(documents)} Markdown documents, {count} relative file links.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
