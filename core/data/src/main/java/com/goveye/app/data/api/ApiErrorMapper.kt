package com.goveye.app.data.api

object ApiErrorMapper {
    fun mapToUserMessage(e: Throwable): String = when (e) {
        is ApiException.HttpError ->
            when (e.code) {
                404 -> "Couldn't find that data. Showing cached data."
                in 500..599 -> "Parliament's service is having issues. Showing cached data."
                else -> "Parliament's service is having issues. Showing cached data."
            }

        is ApiException.NetworkError -> "Couldn't reach Parliament. Showing cached data."

        is ApiException.ParseError -> "Couldn't read Parliament's response. Showing cached data."

        else -> "No data yet. Check your connection."
    }
}
