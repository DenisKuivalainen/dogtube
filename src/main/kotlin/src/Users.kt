package com.example.src

import DBHelpers
import com.example.src.utils.Crypt
import com.example.src.utils.FSHelpers
import com.example.src.utils.Subscription
import com.example.src.utils.blurImage
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.math.min

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

        suspend fun logoutUser(sessionId: String) = response {
            DBHelpers.deleteSession(UUID.fromString(sessionId))
        }

        suspend fun validateSession(sessionId: String) = response<String> {
            DBHelpers.getSession(UUID.fromString(sessionId))
        }

        suspend fun getUserData(username: String) = response<DBHelpers.Companion.User> {
            DBHelpers.getUser(username)
        }

        suspend fun getAllVideos(username: String, searchText: String? = null) =
            response<List<DBHelpers.Companion.VideoData>> {
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

            if (userInDb.subscription_level != "PREMIUM" && videoInDb.isPremium) {
                return@response blurImage(thumbnailBytes)
            }

            thumbnailBytes
        }

        suspend fun subscribe(username: String, cardNumber: String, expiry: String, cvv: String, name: String) =
            response {
                val user = DBHelpers.getUser(username)
                if (user.subscription_level == "PREMIUM") {
                    throw Exception("User already has PREMIUM subscription.")
                }

                Subscription.subscribe(username, cardNumber, expiry, cvv, name)
                DBHelpers.setUserSubscriptionPremium(username)
            }

        private fun parseRangeHeader(rangeHeader: String?, fileLength: Long): Pair<Long, Long> {
            return if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                val range = rangeHeader.removePrefix("bytes=").split("-")
                val start = range[0].toLongOrNull() ?: 0
                val end = range.getOrNull(1)?.toLongOrNull() ?: (fileLength - 1)
                start to min(end, start + 8L * 1024 * 1024 - 1)
            } else {
                0L to 8L * 1024 * 1024 - 1
            }
        }

        @Serializable
        data class StreamVideoResponse(val buffer: ByteArray, val start: Long, val end: Long, val length: Long)
        suspend fun streamVideo(username: String, videoId: String, rangeHeader: String?) = response<StreamVideoResponse> {
            val user = DBHelpers.getUser(username)
            val video = DBHelpers.getVideo(UUID.fromString(videoId))
            if (video.isPremium && user.subscription_level != "PREMIUM") throw Exception("User does not have PREMIUM subscription to watch this video.")

            val fileLength = FSHelpers.getFileSize(videoId)
            val (start, end) = parseRangeHeader(rangeHeader, fileLength)

            StreamVideoResponse(FSHelpers.getVideoChunk(videoId, start, end), start, end, fileLength)
        }

        suspend fun getVideo(username: String, videoId: String) = response<DBHelpers.Companion.VideoData> {
            val user = DBHelpers.getUser(username)
            val video = DBHelpers.getVideoData(username, UUID.fromString(videoId))
            if (video.isPremium && user.subscription_level != "PREMIUM") throw Exception("User does not have PREMIUM subscription to watch this video.")

            video
        }

        suspend fun likeVideo(username: String, videoId: String) = response<DBHelpers.Companion.VideoData> {
            DBHelpers.likeVideo(username, UUID.fromString(videoId))
        }

        suspend fun postMessage(username: String, videoId: String, message: String) = response {
            DBHelpers.postMessage(username, UUID.fromString(videoId), message)
        }

        suspend fun getMessages(username: String, videoId: String) = response {
            DBHelpers.getMessages(username, UUID.fromString(videoId))
        }
    }
}