package com.example.src

import DBHelpers
import com.example.src.utils.Crypt
import com.example.src.utils.FSHelpers
import com.example.src.utils.blurImage
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.util.*

class Users {
    companion object {
        private fun createCookie(sessionId: String) = Cookie(
            name = "user_session",
            value = sessionId,
            maxAge = 1800,
            httpOnly = true,
            path = "/",
            secure = false,
        )

        suspend fun createUser(username: String, name: String, password: String) = response<String> {
            try {
                DBHelpers.getUser(username)
            } catch (e: Exception) {
                println(e)
                // If user was not found

                DBHelpers.createUser(username, name, Crypt.hashPassword(password))

                return@response DBHelpers.createSession(username)
            }

            throw Exception("User already exists.")
        }

        suspend fun loginUser(username: String, password: String) = response<String> {
            val userPasswordInDb = DBHelpers.getUserPassword(username)
            if (!Crypt.checkPassword(password, userPasswordInDb)) throw Exception("User password does not match.")

            DBHelpers.createSession(username)
        }

        suspend fun validateSession(sessionId: String) = response<String> {
            DBHelpers.getSession(UUID.fromString(sessionId))
        }

        suspend fun getUserData(username: String) = response<DBHelpers.Companion.User> {
            DBHelpers.getUser(username)
        }

        suspend fun getAllVideos(username: String, searchText: String? = null) = response<List<DBHelpers.Companion.VideoData>> {
            DBHelpers.getAllVideos(username, searchText)
        }

        @Serializable
        data class OkResponse(val ok: Boolean = true)

        suspend fun createVideoView(username: String, videoId: String) = response<OkResponse> {
            DBHelpers.getVideo(UUID.fromString(videoId))
            DBHelpers.createVideoView(username, UUID.fromString(videoId))
            OkResponse()
        }

        suspend fun getThumbnail(username: String, videoId: String) = response<ByteArray> {
            val thumbnailBytes = FSHelpers.getThumbnail(videoId)

            val userInDb = DBHelpers.getUser(username)
            val videoInDb = DBHelpers.getVideo(UUID.fromString(videoId))

            if(userInDb.subscription_level != "PREMIUM" && videoInDb["is_premium", Boolean::class.java]) {
                return@response blurImage(thumbnailBytes)
            }

            thumbnailBytes
        }
    }
}