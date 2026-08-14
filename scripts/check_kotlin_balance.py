# -*- coding: utf-8 -*-
"""
Contrôle d'équilibrage des délimiteurs sur des fichiers Kotlin.

Filet de sécurité quand la compilation n'est pas disponible localement
(le projet se construit en CI, avec JDK 17 + SDK Android).

Ce n'est PAS un compilateur : ça n'attrape qu'une catégorie d'erreurs — une
parenthèse ou une accolade manquante — mais c'est l'erreur la plus probable
après une génération de code ou une édition en masse.

Le scanner saute correctement les commentaires de ligne, les commentaires de
bloc (imbriqués, comme le permet Kotlin), les chaînes brutes `\"\"\"`, les
chaînes classiques avec échappements et les littéraux de caractère.

Usage : python scripts/check_kotlin_balance.py <fichier.kt> [...]
"""

from __future__ import annotations

import sys
from pathlib import Path

PAIRS = {")": "(", "}": "{", "]": "["}
OPENING = set(PAIRS.values())


def scan(src: str) -> list[str]:
    """Renvoie la liste des anomalies trouvées."""
    errors: list[str] = []
    stack: list[tuple[str, int]] = []
    i, n, line = 0, len(src), 1

    while i < n:
        c = src[i]

        if c == "\n":
            line += 1
            i += 1
        elif src.startswith("//", i):
            i = src.find("\n", i)
            if i == -1:
                break
        elif src.startswith("/*", i):
            depth, i = 1, i + 2
            while i < n and depth:
                if src.startswith("/*", i):
                    depth, i = depth + 1, i + 2
                elif src.startswith("*/", i):
                    depth, i = depth - 1, i + 2
                else:
                    if src[i] == "\n":
                        line += 1
                    i += 1
            if depth:
                errors.append(f"commentaire de bloc non fermé (ligne {line})")
        elif src.startswith('"""', i):
            start, i = line, i + 3
            while i < n and not src.startswith('"""', i):
                if src[i] == "\n":
                    line += 1
                i += 1
            if i >= n:
                errors.append(f"chaîne brute non fermée (ligne {start})")
            i += 3
        elif c == "`":
            # Identifiant échappé : `fun \`fin d'annee\`()`. À traiter avant le
            # littéral de caractère, sans quoi l'apostrophe de « d'annee » ouvre
            # une chaîne fantôme et tout le reste du fichier part de travers.
            # Les noms de tests en français en contiennent constamment.
            start, i = line, i + 1
            while i < n and src[i] not in ("`", "\n"):
                i += 1
            if i >= n or src[i] == "\n":
                errors.append(f"identifiant échappé non fermé (ligne {start})")
            i += 1
        elif c in ('"', "'"):
            quote, start, i = c, line, i + 1
            while i < n and src[i] != quote:
                if src[i] == "\\":
                    i += 1
                elif src[i] == "\n":
                    errors.append(f"chaîne non fermée en fin de ligne {line}")
                    break
                i += 1
            i += 1
        elif c in OPENING:
            stack.append((c, line))
            i += 1
        elif c in PAIRS:
            if not stack:
                errors.append(f"'{c}' orphelin ligne {line}")
            elif stack[-1][0] != PAIRS[c]:
                opener, oline = stack.pop()
                errors.append(f"'{c}' ligne {line} ne ferme pas '{opener}' ligne {oline}")
            else:
                stack.pop()
            i += 1
        else:
            i += 1

    for opener, oline in stack:
        errors.append(f"'{opener}' ligne {oline} jamais fermé")
    return errors


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    failed = 0
    for arg in argv:
        path = Path(arg)
        if not path.exists():
            print(f"  ABSENT  {arg}")
            failed += 1
            continue
        errors = scan(path.read_text(encoding="utf-8"))
        if errors:
            failed += 1
            print(f"  FAIL    {path.name}")
            for e in errors[:10]:
                print(f"            {e}")
        else:
            print(f"  OK      {path.name}")
    print()
    print("Équilibrage OK." if not failed else f"{failed} fichier(s) en échec.")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
