package com.goveye.app.domain.model

data class RepositoryResult<T>(
    val data: T,
    val status: SyncStatus,
)
