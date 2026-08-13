# -*- coding: utf-8 -*-
"""
Vérifications statiques du seed généré, à défaut de pouvoir compiler ici
(le projet se construit en CI : voir .github/workflows).

Contrôle :
  - équilibrage des délimiteurs,
  - littéraux de chaîne bien formés et interpolation Kotlin échappée,
  - toutes les constantes d'enum référencées existent réellement,
  - un visuel présent dans les assets pour chaque exercice,
  - cohérence du nombre d'entrées avec catalog.py.

Usage : python scripts/check_exercise_seed.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from catalog import E, EQUIPMENT_MEDIA  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SEED = ROOT / "app/src/main/java/com/kps/trackmyweight/data/seed/ExerciseSeed.kt"
ENUMS = ROOT / "app/src/main/java/com/kps/trackmyweight/data/db/enums/Enums.kt"
ASSETS = ROOT / "app/src/main/assets/exercises"
EQUIPMENT_ASSETS = ROOT / "app/src/main/assets/equipment"

STRING_LITERAL = re.compile(r'"(?:[^"\\\n]|\\.)*"')

failures: list[str] = []


def check(label: str, ok: bool, detail: str = "") -> None:
    print(f"  {'OK  ' if ok else 'FAIL'}  {label}{(' — ' + detail) if detail else ''}")
    if not ok:
        failures.append(label)


def main() -> int:
    src = SEED.read_text(encoding="utf-8")
    enums = ENUMS.read_text(encoding="utf-8")

    print("Délimiteurs")
    for opening, closing in [("(", ")"), ("{", "}"), ("[", "]")]:
        a, b = src.count(opening), src.count(closing)
        check(f"{opening} {closing}", a == b, f"{a} / {b}")

    print("Chaînes")
    # Retire tous les littéraux bien formés : il ne doit plus rester de guillemet.
    residue = STRING_LITERAL.sub("", src)
    check("littéraux bien formés", '"' not in residue,
          f"{residue.count(chr(34))} guillemet(s) orphelin(s)")
    # L'interpolation n'est légitime que dans le helper `ex(...)` en fin de fichier
    # ("$MEDIA_DIR/$slug"). Dans le bloc de données généré, tout `$` venant de
    # catalog.py doit être échappé, sinon Kotlin tente une interpolation.
    data_block = src[src.index("= listOf("):src.index("\n    )\n")]
    unescaped = [m for m in STRING_LITERAL.findall(data_block) if re.search(r"(?<!\\)\$", m)]
    check("interpolation échappée (données)", not unescaped, str(unescaped[:3]))

    print("Enums")
    for name in ("MuscleGroup", "ExerciseMechanics", "ExerciseForce"):
        block = re.search(r"enum class " + name + r"\s*\{(.*?)\}", enums, re.S)
        if not block:
            check(name, False, "enum introuvable")
            continue
        real = set(re.findall(r"\b[A-Z][A-Z_]*\b", block.group(1)))
        used = set(re.findall(name + r"\.([A-Z_]+)", src))
        check(name, used <= real, f"{len(used)} utilisés, inconnus={sorted(used - real)}")

    print("Contenu")
    calls = len(re.findall(r"^        ex\($", src, re.M))
    check("nombre d'entrées", calls == len(E), f"{calls} dans le .kt / {len(E)} dans catalog.py")
    slugs_kt = re.findall(r'^            "([a-z0-9_]+)", "', src, re.M)
    check("slugs uniques", len(set(slugs_kt)) == len(slugs_kt))
    check("slugs conformes au catalogue", set(slugs_kt) == {e[0] for e in E})

    print("Assets")
    missing = [
        f"{e[0]}_{i}.webp"
        for e in E for i in (0, 1)
        if not (ASSETS / f"{e[0]}_{i}.webp").exists()
    ]
    total_mb = sum(p.stat().st_size for p in ASSETS.glob("*.webp")) / 1024 / 1024
    check("visuels présents", not missing, f"{len(missing)} manquant(s)")
    orphans = {p.stem.rsplit("_", 1)[0] for p in ASSETS.glob("*.webp")} - {e[0] for e in E}
    check("pas de visuel orphelin", not orphans, str(sorted(orphans)[:5]))
    print(f"        {len(list(ASSETS.glob('*.webp')))} fichiers, {total_mb:.2f} Mo")

    missing_eq = [k for k in EQUIPMENT_MEDIA if not (EQUIPMENT_ASSETS / f"{k}.webp").exists()]
    eq_mb = sum(p.stat().st_size for p in EQUIPMENT_ASSETS.glob("*.webp")) / 1024 / 1024
    check("visuels d'équipement", not missing_eq, f"{len(missing_eq)} manquant(s)")
    covered = re.findall(r'^        "([a-z_]+)",$', (
        ROOT / "app/src/main/java/com/kps/trackmyweight/data/seed/EquipmentMedia.kt"
    ).read_text(encoding="utf-8"), re.M)
    check("EquipmentMedia.kt synchronisé", set(covered) == set(EQUIPMENT_MEDIA),
          f"{len(covered)} dans le .kt / {len(EQUIPMENT_MEDIA)} dans catalog.py")
    print(f"        {len(list(EQUIPMENT_ASSETS.glob('*.webp')))} fichiers, {eq_mb:.2f} Mo")

    print()
    if failures:
        print(f"{len(failures)} CONTRÔLE(S) EN ÉCHEC : {failures}")
        return 1
    print("Tous les contrôles passent.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
