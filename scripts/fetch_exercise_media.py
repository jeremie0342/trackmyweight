# -*- coding: utf-8 -*-
"""
Importe les visuels d'exercices depuis free-exercise-db vers les assets de l'app.

Chaque exercice de `catalog.py` a exactement deux images dans la source :
la position de départ (_0) et la position finale (_1). L'app les fait alterner
en fondu pour obtenir une démonstration animée sans embarquer de GIF.

Usage :
    python scripts/fetch_exercise_media.py            # n'écrit que ce qui manque
    python scripts/fetch_exercise_media.py --force    # réécrit tout
    python scripts/fetch_exercise_media.py --width 512 --quality 75

Dépendance : Pillow (pip install Pillow)

Source : https://github.com/yuhonas/free-exercise-db — The Unlicense (domaine public).
"""

from __future__ import annotations

import argparse
import io
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from catalog import E, EQUIPMENT_MEDIA  # noqa: E402

RAW_BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"
ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT_DIR = ASSETS / "exercises"
EQUIPMENT_DIR = ASSETS / "equipment"


def fetch(url: str, retries: int = 3) -> bytes:
    last: Exception | None = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "trackmyweight-import"})
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.read()
        except Exception as exc:  # réseau capricieux : on retente
            last = exc
    raise RuntimeError(f"échec du téléchargement {url}: {last}")


def convert(raw: bytes, width: int, quality: int) -> bytes:
    img = Image.open(io.BytesIO(raw)).convert("RGB")
    if img.width > width:
        height = round(img.height * width / img.width)
        img = img.resize((width, height), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format="WEBP", quality=quality, method=6)
    return buf.getvalue()


def process(entry, width: int, quality: int, force: bool) -> tuple[str, int, int]:
    """Renvoie (slug, octets écrits, nombre d'images ignorées)."""
    slug, src = entry[0], entry[1]
    written = 0
    skipped = 0
    for index in (0, 1):
        dest = OUT_DIR / f"{slug}_{index}.webp"
        if dest.exists() and not force:
            skipped += 1
            continue
        data = convert(fetch(f"{RAW_BASE}/{src}/{index}.jpg"), width, quality)
        dest.write_bytes(data)
        written += len(data)
    return slug, written, skipped


def process_equipment(item, width: int, quality: int, force: bool) -> tuple[str, int, int]:
    """Un seul visuel par équipement : on identifie une machine, on n'anime rien."""
    key, src = item
    dest = EQUIPMENT_DIR / f"{key}.webp"
    if dest.exists() and not force:
        return key, 0, 1
    data = convert(fetch(f"{RAW_BASE}/{src}/0.jpg"), width, quality)
    dest.write_bytes(data)
    return key, len(data), 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--width", type=int, default=640)
    parser.add_argument("--quality", type=int, default=80)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--workers", type=int, default=8)
    args = parser.parse_args()

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    EQUIPMENT_DIR.mkdir(parents=True, exist_ok=True)
    print(f"{len(E)} exercices -> {OUT_DIR}")
    print(f"{len(EQUIPMENT_MEDIA)} équipements -> {EQUIPMENT_DIR}")

    total_bytes = 0
    total_skipped = 0
    failures: list[tuple[str, str]] = []

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {
            pool.submit(process, e, args.width, args.quality, args.force): e[0]
            for e in E
        }
        for done, (future, slug) in enumerate(futures.items(), start=1):
            try:
                _, written, skipped = future.result()
                total_bytes += written
                total_skipped += skipped
            except Exception as exc:
                failures.append((slug, str(exc)))
            if done % 25 == 0 or done == len(futures):
                print(f"  {done}/{len(futures)}")

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {
            pool.submit(process_equipment, item, args.width, args.quality, args.force): item[0]
            for item in EQUIPMENT_MEDIA.items()
        }
        for future, key in futures.items():
            try:
                _, written, skipped = future.result()
                total_bytes += written
                total_skipped += skipped
            except Exception as exc:
                failures.append((f"equipment/{key}", str(exc)))

    exercises_size = sum(p.stat().st_size for p in OUT_DIR.glob("*.webp"))
    equipment_size = sum(p.stat().st_size for p in EQUIPMENT_DIR.glob("*.webp"))
    print(f"\nécrits     : {total_bytes / 1024:.0f} Ko")
    print(f"ignorés    : {total_skipped} images déjà présentes")
    print(f"exercices  : {len(list(OUT_DIR.glob('*.webp')))} fichiers, {exercises_size / 1024 / 1024:.2f} Mo")
    print(f"équipements: {len(list(EQUIPMENT_DIR.glob('*.webp')))} fichiers, {equipment_size / 1024 / 1024:.2f} Mo")
    print(f"total      : {(exercises_size + equipment_size) / 1024 / 1024:.2f} Mo")

    if failures:
        print(f"\n{len(failures)} ÉCHECS :")
        for slug, err in failures:
            print(f"  {slug}: {err}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
