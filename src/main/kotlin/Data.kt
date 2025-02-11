package com.example

import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

data class AppPrincipal(val key: String) : Principal
data class UserPrincipal(val username: String) : Principal

@Serializable
data class AdminUserCredentials(val username: String, val password: String)

@Serializable
data class PostVideoRequest(val name: String, val isPremium: Boolean, val bufferSize: Int, val extension: String)
