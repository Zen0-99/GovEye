package com.goveye.app.data.update

/**
 * Sealed state for the database update flow (DATA-03, D-10, D-10a).
 *
 * Drives the UI in [com.goveye.app.MainActivity] and the
 * [com.goveye.app.work.DatabaseUpdateWorker] logic.
 *
 * The 5-patch-stream architecture (D-10a) means [NeedsPatches] carries a list
 * of [PatchInfo] — one per stream that has an available patch.
 */
sealed interface DatabaseUpdateState {
    /** Initial state — no check has been performed yet. */
    data object Idle : DatabaseUpdateState

    /** Manifest check in progress. */
    data object Checking : DatabaseUpdateState

    /** All 5 streams are up to date — no action needed. */
    data object UpToDate : DatabaseUpdateState

    /**
     * One or more streams have patches available (D-10a).
     *
     * [patches] contains one [PatchInfo] per stream that needs updating.
     * Patches are tiny (5-50KB each, up to 5 = max 250KB) and are applied
     * in a single Room transaction.
     */
    data class NeedsPatches(val patches: List<PatchInfo>) : DatabaseUpdateState

    /**
     * First launch (seed version is null) or a stream is multiple versions
     * behind — full per-API DB download required (D-04, D-05, D-10a).
     * The app downloads all 7 per-API .db files and merges them on-device.
     */
    data class NeedsFullDownload(val seedManifest: DatabaseManifest?) : DatabaseUpdateState

    /** Download in progress — [progress] is 0f..1f, [isFullDb] distinguishes DB vs patch. */
    data class Downloading(val progress: Float, val isFullDb: Boolean) : DatabaseUpdateState

    /** Patch is being applied via Room transaction. */
    data object Applying : DatabaseUpdateState

    /** An error occurred — [message] describes the failure. */
    data class Failed(val message: String) : DatabaseUpdateState

    /** Metered connection detected before a full DB download (Pitfall 4). */
    data object NeedsWifi : DatabaseUpdateState

    /**
     * Seed DB download is complete, but Room's InvalidationTracker is broken
     * from `database.close()` during the download. The user must tap
     * "Restart" to recreate the Activity so all ViewModels and Flow
     * collectors get fresh Room connections.
     */
    data object NeedsRestart : DatabaseUpdateState
}

/**
 * Information about a single patch stream that has an available patch (D-10a).
 *
 * @param streamName The stream identifier: "mps", "commons-votes", "lords-votes", "bills", "committees", or "recess".
 * @param manifest The manifest from the per-API release (contains version, patch info).
 * @param patchUrl Direct download URL for the patch.json file.
 */
data class PatchInfo(val streamName: String, val manifest: DatabaseManifest, val patchUrl: String)
