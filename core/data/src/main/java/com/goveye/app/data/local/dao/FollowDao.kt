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

    @Query("SELECT memberId FROM follows WHERE isMuted = 0")
    fun observeUnmutedMemberIds(): Flow<List<Int>>

    @Query("SELECT memberId FROM follows WHERE isMuted = 0")
    suspend fun getUnmutedMemberIds(): List<Int>

    @Query("SELECT COUNT(*) FROM follows")
    suspend fun getFollowCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE memberId = :memberId")
    suspend fun delete(memberId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE memberId = :memberId)")
    suspend fun isFollowing(memberId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE memberId = :memberId)")
    fun observeIsFollowing(memberId: Int): Flow<Boolean>

    @Query("SELECT isMuted FROM follows WHERE memberId = :memberId")
    fun observeIsMuted(memberId: Int): Flow<Boolean?>

    @Query("UPDATE follows SET isMuted = :isMuted WHERE memberId = :memberId")
    suspend fun setMuted(memberId: Int, isMuted: Boolean)
}
