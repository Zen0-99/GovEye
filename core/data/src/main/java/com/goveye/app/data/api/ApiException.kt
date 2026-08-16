package com.goveye.app.data.api

sealed class ApiException(message: String) : Exception(message) {
    data class HttpError(val code: Int, val userMessage: String) : ApiException(userMessage)

    data class NetworkError(val userMessage: String) : ApiException(userMessage)

    data class ParseError(val userMessage: String) : ApiException(userMessage)
}
