package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM remote_keys WHERE label = :label")
    suspend fun getRemoteKey(label: String): RemoteKeyEntity?

    @Upsert
    suspend fun upsert(remoteKey: RemoteKeyEntity)

    @Query("DELETE FROM remote_keys WHERE label = :label")
    suspend fun deleteByLabel(label: String)
}
