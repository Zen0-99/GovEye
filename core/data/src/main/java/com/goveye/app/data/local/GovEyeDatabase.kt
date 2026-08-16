package com.goveye.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpFtsEntity

/**
 * Room database for GovEye (D-28).
 *
 * Phase 1 scaffold — the schema will expand in Phase 2 with additional
 * entities for divisions, bills, votes, and follow relationships.
 */
@Database(
    entities = [
        MpEntity::class,
        MpFtsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GovEyeDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao

    companion object {
        const val DATABASE_NAME = "goveye.db"
    }
}
