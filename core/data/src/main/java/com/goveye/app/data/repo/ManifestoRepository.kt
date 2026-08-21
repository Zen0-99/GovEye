package com.goveye.app.data.repo

import android.util.Log
import com.goveye.app.data.local.dao.ManifestoDao
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.domain.search.FtsQuerySanitizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestoRepository @Inject constructor(private val manifestoDao: ManifestoDao) {

    companion object {
        private const val TAG = "GovEye/Manifesto"
    }

    suspend fun getManifesto(partyId: Int): PartyManifestoEntity? = manifestoDao.getManifesto(partyId)

    suspend fun getManifestoText(partyId: Int): String? = manifestoDao.getManifestoText(partyId)

    suspend fun searchManifesto(partyId: Int, query: String): List<ManifestoSearchResult> {
        val sanitized = FtsQuerySanitizer.sanitize(query) ?: return emptyList()

        return try {
            val results = manifestoDao.searchManifestoFts4(partyId, sanitized)
            Log.i(TAG, "FTS4 search for partyId=$partyId query='$sanitized': ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e(TAG, "FTS4 search failed: ${e.message}")
            emptyList()
        }
    }
}
