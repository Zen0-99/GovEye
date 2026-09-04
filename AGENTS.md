# GovEye — Project Notes

## Loading Screen Design Preferences
- **Background**: Same as app (`MaterialTheme.colorScheme.background`), no gradient
- **No decorative icons**: No cloud/download icons during progress states
- **Circular progress**: Large (280dp), thick stroke (12dp), percentage text at 64sp in center
- **Layout**: Title "GovEye" + subtitle at top, circular progress centered, "This only happens once" at bottom
- **Consistent size**: Indeterminate spinner must match determinate circle size (280dp) so layout doesn't jump

## Build & Test
- `.\gradlew.bat spotlessApply :app:installDebug` — build, format, install on device
- **NEVER run `adb shell pm clear com.goveye.app`** — it forces the user through onboarding again. Install over the existing app instead.
- **NEVER push a goveye.db directly to the device** — the app's patch system tracks per-stream versions. A manually pushed DB (even at the right schema version) will have mismatched stream versions, causing patches to corrupt data. To update the seed on the device: upload to GitHub seed-latest release, bump `CURRENT_SEED_VERSION` in `DatabaseUpdateManager.kt`, and let the app download it. For testing only: push the DB AND clear `database_preferences.preferences_pb` so the app does a full re-download.
- `adb logcat -d -s GovEye/DbUpdate:I GovEye/MainActivity:I AndroidRuntime:E` — check logs

## Data Pipeline
The bundled seed DB is built in the [goveye-data](https://github.com/Zen0-99/goveye-data) repo.
See `goveye-data/AGENTS.md` for the full pipeline guide — build scripts, per-API DBs, merge order,
and the critical "when NOT to run a build script" decision guide (derived column changes vs
source data changes).

When adding a Room migration that changes derived data (e.g. re-mapping a `bucket` column),
the same SQL should be run against the local per-API DB in goveye-data — do NOT re-run the
build script (it re-fetches all 650 MPs from the API unnecessarily).
