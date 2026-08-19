package com.goveye.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.entity.BillFollowEntity
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity

/**
 * Room database for the 3 user-data tables (D-10a).
 *
 * This database holds user-specific data: followed MPs ([FollowEntity]),
 * followed bills ([BillFollowEntity]), and per-MP notification preferences
 * ([MpNotificationPreferenceEntity]).
 *
 * It is created fresh on first launch by Room (no pre-placed file needed) and
 * is NEVER patched — user data persists across all DB updates and seed DB
 * swaps. The seed DB swap only replaces goveye.db ([BundledDatabase]),
 * not local.db.
 *
 * None of these entities use types requiring [Converters] (only primitives and
 * Boolean), so no @TypeConverters annotation is needed.
 */
@Database(
    entities = [
        FollowEntity::class,
        BillFollowEntity::class,
        MpNotificationPreferenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun followDao(): FollowDao
    abstract fun billFollowDao(): BillFollowDao
    abstract fun mpNotificationPreferenceDao(): MpNotificationPreferenceDao

    companion object {
        const val DATABASE_NAME = "local.db"
    }
}
