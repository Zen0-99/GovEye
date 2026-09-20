package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpAppointmentEntity

@Dao
interface MpAppointmentDao {
    @Query("SELECT * FROM mp_appointments WHERE mpId = :mpId ORDER BY isCurrent DESC, appointedOn DESC")
    suspend fun getByMpId(mpId: Int): List<MpAppointmentEntity>

    @Upsert
    suspend fun upsertAll(data: List<MpAppointmentEntity>)
}
