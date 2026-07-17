# Documentation & assets

This folder holds screenshots, illustrations and additional documentation for the project.

## Contributing a screenshot

The 6 missing captures for the main README are:

| Expected filename | Screen to capture | What to show |
|---|---|---|
| `screenshots/01-today.png` | **Today** tab | Filled readiness card + daily macros + a few habits checked |
| `screenshots/02-workout.png` | **Workout** tab | Templates listed + recent PRs + history |
| `screenshots/03-nutrition.png` | **Nutrition** tab | Macro progress + one meal with 2-3 items |
| `screenshots/04-body.png` | **Body** tab | Hub with 3 cards (Weight / Measurements / Photos) |
| `screenshots/05-report.png` | **Weekly report** screen | Adherence + narrative + a Coach advice visible |
| `screenshots/06-widget.png` | Android home screen | Widget showing last weigh-in + the "+ Add" button |

### Technical spec

- Format: PNG preferred (JPEG at 90% quality OK)
- Ratio: **portrait 9:19** or your device's native ratio
- Target width: **1080 px** (resize if larger)
- Max size: **500 KB** per image
- **No personal info** visible (a real weight is fine if you're OK with it, no name/email)

### How to capture

**From the phone**: volume-down + power → crop if needed → transfer via cable/cloud.

**From a PC via ADB**:

```bash
adb shell screencap -p /sdcard/tmw.png
adb pull /sdcard/tmw.png
adb shell rm /sdcard/tmw.png
```

### Submit

1. Fork the repo
2. Add your captures under `docs/screenshots/`
3. Open a PR with the description "docs: add screenshot for [screen name]"

## Other assets

- `logo/` — logo variants (TBD)
- `press-kit/` — press kit for blogs/reviews (TBD)
