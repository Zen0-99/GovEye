package com.goveye.app.data.update

/**
 * Sealed state for the database update flow (DATA-03).
 *
 * Drives the UI in [com.goveye.app.MainActivity] and the
 * [com.goveye.app.work.DatabaseUpdateWorker] logic.
 */
sealed interface DatabaseUpdateState {
    /** Initial state — no check has been performed yet. */
    data object Idle : DatabaseUpdateState

    /** Manifest check in progress. */
    data object Checking : DatabaseUpdateState

    /** Local DB version matches manifest version — no action needed. */
    data object UpToDate : DatabaseUpdateState

    /** Local version is exactly 1 behind — a patch (5-50KB) can be applied (D-05). */
    data class NeedsPatch(val manifest: DatabaseManifest) : DatabaseUpdateState

    /**
     * First launch (manifest is null) or local version is multiple behind —
     * full DB download (~160MB) required (D-04, D-05).
     */
    data class NeedsFullDownload(val manifest: DatabaseManifest?) : DatabaseUpdateState

    /** Download in progress — [progress] is 0f..1f, [isFullDb] distinguishes DB vs patch. */
    data class Downloading(val progress: Float, val isFullDb: Boolean) : DatabaseUpdateState

    /** Patch is being applied via Room transaction. */
    data object Applying : DatabaseUpdateState

    /** An error occurred — [message] describes the failure. */
    data class Failed(val message: String) : DatabaseUpdateState

    /** Metered connection detected before a full DB download (Pitfall 4). */
    data object NeedsWifi : DatabaseUpdateState
}
