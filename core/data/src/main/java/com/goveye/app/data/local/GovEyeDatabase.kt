package com.goveye.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.RemoteKeyDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillFollowEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpFtsEntity
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import com.goveye.app.data.local.entity.RemoteKeyEntity

@Database(
    entities = [
        MpEntity::class,
        MpFtsEntity::class,
        RemoteKeyEntity::class,
        CommitteeEntity::class,
        MpCommitteeCrossRef::class,
        DivisionEntity::class,
        DivisionVoteEntity::class,
        BillEntity::class,
        BillStageEntity::class,
        BillFollowEntity::class,
        HansardContributionEntity::class,
        InterestEntity::class,
        FollowEntity::class,
        RecessDateEntity::class,
        RecessDatesMetaEntity::class,
        MpNotificationPreferenceEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GovEyeDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao
    abstract fun mpDao(): MpDao
    abstract fun remoteKeyDao(): RemoteKeyDao
    abstract fun committeeDao(): CommitteeDao
    abstract fun divisionDao(): DivisionDao
    abstract fun billDao(): BillDao
    abstract fun billFollowDao(): BillFollowDao
    abstract fun hansardDao(): HansardDao
    abstract fun interestDao(): InterestDao
    abstract fun followDao(): FollowDao
    abstract fun recessDateDao(): RecessDateDao
    abstract fun mpNotificationPreferenceDao(): MpNotificationPreferenceDao

    companion object {
        const val DATABASE_NAME = "goveye.db"
    }
}
