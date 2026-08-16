package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goveye.app.data.local.entity.FollowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {
    @Query("SELECT * FROM follows ORDER BY followedAt DESC")
    fun observeAllFollows(): Flow<List<FollowEntity>>

    @Query("SELECT memberId FROM follows ORDER BY followedAt DESC")
    fun observeFollowedMemberIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE memberId = :memberId")
    suspend fun delete(memberId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE memberId = :memberId)")
    suspend fun isFollowing(memberId: Int): Boolean
}
