# -*- coding: utf-8 -*-
"""
Génère ExerciseSeed.kt à partir de la table de curation `catalog.py`.

Le fichier Kotlin produit est du code généré : ne pas l'éditer à la main,
modifier `catalog.py` puis relancer ce script.

Usage :
    python scripts/generate_exercise_seed.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from catalog import E, EQUIPMENT_MEDIA  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SEED_DIR = ROOT / "app" / "src" / "main" / "java" / "com" / "kps" / "trackmyweight" / "data" / "seed"
DEST = SEED_DIR / "ExerciseSeed.kt"
EQUIPMENT_DEST = SEED_DIR / "EquipmentMedia.kt"

# Ordre d'affichage des sections, par muscle principal.
SECTIONS = [
    ("PECTORAUX", ["CHEST"]),
    ("DOS", ["BACK_LATS", "BACK_UPPER", "LOWER_BACK"]),
    ("TRAPÈZES", ["TRAPS"]),
    ("ÉPAULES", ["SHOULDERS_FRONT", "SHOULDERS_SIDE", "SHOULDERS_REAR"]),
    ("BICEPS", ["BICEPS"]),
    ("TRICEPS", ["TRICEPS"]),
    ("AVANT-BRAS", ["FOREARMS"]),
    ("QUADRICEPS", ["QUADS"]),
    ("ISCHIOS / FESSIERS", ["HAMSTRINGS", "GLUTES"]),
    ("MOLLETS", ["CALVES"]),
    ("ABDOMINAUX / OBLIQUES", ["ABS", "OBLIQUES"]),
    ("NUQUE", ["NECK"]),
]

HEADER = '''package com.kps.trackmyweight.data.seed

import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.datetime.Instant

/**
 * FICHIER GÉNÉRÉ — ne pas éditer à la main.
 *
 * Source de vérité : `scripts/catalog.py`.
 * Régénérer avec : `python scripts/generate_exercise_seed.py`
 *
 * Référentiel de {count} exercices seedé au premier lancement, avec pour chacun :
 *  - les muscles primaire et secondaires, la mécanique et le type de force,
 *  - les équipements requis (par clé — voir [EquipmentSeed]),
 *  - une consigne technique courte ([ExerciseEntity.cues]),
 *  - le chemin des visuels ([ExerciseEntity.mediaPath]).
 *
 * Les visuels sont dans `assets/exercises/` sous la forme `<slug>_0.webp`
 * (position de départ) et `<slug>_1.webp` (position finale). L'UI alterne les
 * deux en fondu pour animer la démonstration.
 *
 * Visuels et données de référence issus de https://github.com/yuhonas/free-exercise-db
 * (The Unlicense — domaine public).
 */
object ExerciseSeed {

    /** Racine des visuels dans les assets. Voir [ExerciseEntity.mediaPath]. */
    const val MEDIA_DIR: String = "exercises"

    /** Suffixe de l'image de position de départ. */
    const val MEDIA_START_SUFFIX: String = "_0.webp"

    /** Suffixe de l'image de position finale. */
    const val MEDIA_END_SUFFIX: String = "_1.webp"

    fun items(now: Instant): List<Pair<ExerciseEntity, List<String>>> = listOf(
'''

FOOTER = '''    )

    private fun ex(
        slug: String,
        name: String,
        primary: MuscleGroup,
        secondary: List<MuscleGroup>,
        mechanics: ExerciseMechanics,
        force: ExerciseForce,
        equipmentKeys: List<String>,
        cues: String,
        now: Instant,
    ): Pair<ExerciseEntity, List<String>> = ExerciseEntity(
        name = name,
        slug = slug,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        mechanics = mechanics,
        force = force,
        defaultRestSec = when (mechanics) {
            ExerciseMechanics.COMPOUND -> 180
            ExerciseMechanics.ISOLATION -> 90
        },
        cues = cues,
        mediaPath = "$MEDIA_DIR/$slug",
        createdAt = now,
        updatedAt = now,
    ) to equipmentKeys
}
'''


def kstr(s: str) -> str:
    """Littéral Kotlin sûr : échappe backslash, guillemet et interpolation."""
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def klist(items: list[str], prefix: str) -> str:
    if not items:
        return "emptyList()"
    return "listOf(" + ", ".join(f"{prefix}.{i}" for i in items) + ")"


def kequip(items: list[str]) -> str:
    if not items:
        return "emptyList()"
    return "listOf(" + ", ".join(kstr(i) for i in items) + ")"


def main() -> int:
    by_slug = {e[0]: e for e in E}
    emitted: set[str] = set()
    out: list[str] = [HEADER.replace("{count}", str(len(E)))]

    for title, muscles in SECTIONS:
        rows = [e for e in E if e[3] in muscles]
        if not rows:
            continue
        bar = "─" * max(3, (60 - len(title)) // 2)
        out.append(f"        // {bar} {title} {bar}\n")
        for slug, src, name, primary, secondary, mech, force, equip, cues in rows:
            if slug in emitted:
                raise SystemExit(f"slug émis deux fois : {slug}")
            emitted.add(slug)
            out.append(
                f"        ex(\n"
                f"            {kstr(slug)}, {kstr(name)},\n"
                f"            MuscleGroup.{primary}, {klist(secondary, 'MuscleGroup')},\n"
                f"            ExerciseMechanics.{mech}, ExerciseForce.{force},\n"
                f"            {kequip(equip)},\n"
                f"            {kstr(cues)},\n"
                f"            now,\n"
                f"        ),\n"
            )
        out.append("\n")

    missing = set(by_slug) - emitted
    if missing:
        raise SystemExit(f"exercices non émis (muscle absent de SECTIONS) : {sorted(missing)}")

    out.append(FOOTER)
    DEST.write_text("".join(out), encoding="utf-8")
    print(f"{DEST.relative_to(ROOT)} — {len(emitted)} exercices, {DEST.stat().st_size / 1024:.1f} Ko")

    write_equipment_media()
    return 0


def write_equipment_media() -> None:
    keys = "\n".join(f"        {kstr(k)}," for k in sorted(EQUIPMENT_MEDIA))
    EQUIPMENT_DEST.write_text(
        f'''package com.kps.trackmyweight.data.seed

/**
 * FICHIER GÉNÉRÉ — ne pas éditer à la main.
 * Source de vérité : `scripts/catalog.py` (EQUIPMENT_MEDIA).
 * Régénérer avec : `python scripts/generate_exercise_seed.py`
 *
 * Équipements disposant d'un visuel dans `assets/equipment/<clé>.webp`.
 *
 * Les accessoires sans visuel distinctif (ceinture, sangles, foam roller,
 * micro-poids, barres courtes, haltères réglables) en sont volontairement
 * absents : l'UI se contente alors d'afficher leur nom.
 */
object EquipmentMedia {{

    /** Racine des visuels d'équipement dans les assets. */
    const val DIR: String = "equipment"

    private val COVERED: Set<String> = setOf(
{keys}
    )

    /**
     * Chemin du visuel dans les assets, ou `null` si cet équipement n'en a pas.
     * Les appelants doivent gérer le `null` plutôt que de supposer une image.
     */
    fun pathFor(equipmentKey: String): String? =
        if (equipmentKey in COVERED) "$DIR/$equipmentKey.webp" else null
}}
''',
        encoding="utf-8",
    )
    print(f"{EQUIPMENT_DEST.relative_to(ROOT)} — {len(EQUIPMENT_MEDIA)} équipements")


if __name__ == "__main__":
    raise SystemExit(main())
