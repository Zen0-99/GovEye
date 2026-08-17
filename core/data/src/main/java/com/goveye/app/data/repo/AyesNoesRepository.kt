package com.goveye.app.data.repo

import com.goveye.app.data.api.AyesNoesApi
import com.goveye.app.data.dto.ayesnoes.AyesMpDetail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AyesNoesRepository @Inject constructor(
    private val api: AyesNoesApi,
) {
    suspend fun getMp(memberId: Int): AyesMpDetail? =
        try {
            api.getMp(memberId).mp
        } catch (e: Exception) {
            null
        }
}
