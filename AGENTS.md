# GovEye — Project Notes

## Loading Screen Design Preferences
- **Background**: Same as app (`MaterialTheme.colorScheme.background`), no gradient
- **No decorative icons**: No cloud/download icons during progress states
- **Circular progress**: Large (280dp), thick stroke (12dp), percentage text at 64sp in center
- **Layout**: Title "GovEye" + subtitle at top, circular progress centered, "This only happens once" at bottom
- **Consistent size**: Indeterminate spinner must match determinate circle size (280dp) so layout doesn't jump

## Build & Test
- `.\gradlew.bat spotlessApply :app:installDebug` — build, format, install on device
- `adb shell pm clear com.goveye.app` — clear app data (simulate first launch)
- `adb logcat -d -s GovEye/DbUpdate:I GovEye/MainActivity:I AndroidRuntime:E` — check logs
