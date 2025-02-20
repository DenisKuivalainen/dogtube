package com.example

import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

data class AppPrincipal(val key: String) : Principal
data class UserPrincipal(val username: String) : Principal
data class UserSessionPrincipal(val username: String, val session: String) : Principal

@Serializable
data class AdminUserCredentials(val username: String, val password: String)

@Serializable
data class PostVideoRequest(val name: String, val isPremium: Boolean, val bufferSize: Int, val extension: String)

@Serializable
data class CreateUserRequest(val username: String, val name: String, val password: String)

@Serializable
data class LoginUserRequest(val username: String, val password: String)

@Serializable
data class EditVideoRequest(val name: String?, val isPremium: Boolean?)
