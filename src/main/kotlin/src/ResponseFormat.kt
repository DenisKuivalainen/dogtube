package com.example.src

import ioOperation
import kotlinx.serialization.Serializable

@Serializable
sealed class ResponseFormat<T>(
    open val data: T? = null,
    open val error: String? = null
)

data class SuccessResponse<T>(
    override val data: T,
) : ResponseFormat<T>(data)

data class FailResponse<T>(
    override val error: String? = "Something went wrong."
) : ResponseFormat<T>(data = null, error)

suspend fun <T> response(fn: suspend () -> T): ResponseFormat<T> {
    return try {
          SuccessResponse(data = fn())
    }catch (e: Exception) {
        println(e.message)
        FailResponse(error = e.message)
    }
}