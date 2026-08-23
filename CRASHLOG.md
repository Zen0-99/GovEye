# GovEye — Crash Log

## Crash 1: OutOfMemoryError during seed DB download
**Date:** 2025-08-22
**Symptom:** App crashed with `OutOfMemoryError: Failed to allocate a 8208 byte allocation` during seed DB download (557 MB).
**Root cause:** `HttpLoggingInterceptor` with `Level.BODY` was inherited by `dbDownloadClient` via `okHttpClient.newBuilder()`. The BODY logging level buffers the entire response body in memory for logging, which causes OOM on large file downloads.
**Fix:** `dbDownloadClient` in `NetworkModule.kt` is now built from scratch (not via `newBuilder()`) without any logging interceptor — only User-Agent header + 10-minute timeouts.
**File:** `app/src/main/java/com/goveye/app/di/NetworkModule.kt`

## Crash 2: App vanishes after seed DB download completes
**Date:** 2025-08-22
**Symptom:** Seed DB download succeeds (584 MB, ~42s), but app disappears from screen. Android restarts only the WorkManager service, not the Activity.
**Root cause:** `DatabaseDownloadWorker` called `Process.killProcess(Process.myPid())` after download completion to force a fresh Room instance (InvalidationTracker was broken from `database.close()`). However, `killProcess` kills the entire process and Android only restarts the WorkManager bound service — the Activity is force-removed with no saved state and not relaunched.
**Fix:** Replaced `Process.killProcess()` with `context.startActivity(Intent for MainActivity with FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK)`. This recreates the Activity and all ViewModels, which creates new Flow collectors that trigger Room to reopen with a fresh InvalidationTracker.
**File:** `app/src/main/java/com/goveye/app/work/DatabaseDownloadWorker.kt`
