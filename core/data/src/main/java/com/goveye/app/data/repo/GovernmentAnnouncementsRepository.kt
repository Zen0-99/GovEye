package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.WrittenStatementDao
import com.goveye.app.data.local.entity.WrittenStatementEntity
import com.goveye.app.domain.model.WrittenStatement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DB-only repository for government announcements (publications, written
 * statements, legislation, tags, MP tags, party leaders, source
 * recommendations).
 *
 * No Retrofit/API imports — all data comes from [BundledDatabase] tables
 * populated at build time by the goveye-data build scripts.
 *
 * Follows the [InterestsRepository] pattern.
 */
@Singleton
class GovernmentAnnouncementsRepository @Inject constructor(
    private val writtenStatementDao: WrittenStatementDao
) {
    fun observeStatements(limit: Int = 50): Flow<List<WrittenStatement>> =
        writtenStatementDao.observeStatements(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeStatementsByHouse(house: Int, limit: Int = 50): Flow<List<WrittenStatement>> =
        writtenStatementDao.observeStatementsByHouse(house, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchStatements(query: String, limit: Int = 50): Flow<List<WrittenStatement>> =
        writtenStatementDao.searchStatements(query, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getStatement(id: Int): WrittenStatement? =
        writtenStatementDao.getStatement(id)?.toDomain()

    private fun WrittenStatementEntity.toDomain(): WrittenStatement = WrittenStatement(
        id = id,
        memberId = memberId,
        memberRole = memberRole,
        uin = uin,
        dateMade = dateMade,
        answeringBodyId = answeringBodyId,
        answeringBodyName = answeringBodyName,
        title = title,
        text = text,
        house = house
    )
}
