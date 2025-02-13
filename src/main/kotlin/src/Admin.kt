package com.example.src

import DBHelpers
import VideoStatus
import com.example.src.utils.*
import kotlinx.serialization.Serializable
import java.util.*

class Admin {
    companion object {
        @Serializable
        data class AdminUser(val username: String)

        suspend fun getAdminUser(username: String) = response<AdminUser> {
            val usernameInDb = DBHelpers.getAdminUser(username).get(AdminUsers.USERNAME)

            AdminUser(usernameInDb)
        }

        suspend fun createAdminUser(username: String, password: String) = response {
            try {
                DBHelpers.getAdminUser(username)
                throw Exception("User already exists.")
            } catch (_: Exception) {
                // If user was not found

                val hashedPassword = Crypt.hashPassword(password)
                DBHelpers.createAdminUser(username, hashedPassword)
            }
        }

        @Serializable
        data class JwtResponse(val jwt: String) {
            companion object {
                fun fromUsername(username: String): JwtResponse {
                    return JwtResponse(JWT.create(username))
                }
            }
        }

        suspend fun authAdminUser(username: String, password: String) = response<JwtResponse> {
            val userInDb = DBHelpers.getAdminUser(username)

            val hashedPassword = userInDb.get(AdminUsers.PASSWORD)

            if (!Crypt.checkPassword(password, hashedPassword ?: "")) {
                throw Exception("Password do not match")
            }

            JwtResponse.fromUsername(userInDb.get(AdminUsers.USERNAME))
        }

        suspend fun createVideo(
            name: String, isPremium: Boolean, bufferSize: Int, extension: String
        ) = response<DBHelpers.Companion.VideoCreationResponse> {
            val res = DBHelpers.createVideo(name, isPremium, bufferSize, extension)

            FSHelpers.createMockFile(res.id, extension, bufferSize)


            res
        }

        suspend fun uploadVideoChunk(_id: String, _chunkId: String, chunk: ByteArray) = response {
            val id = UUID.fromString(_id)
            val chunkId = UUID.fromString(_chunkId)

            val videoInDb = DBHelpers.getVideo(id)
            if (videoInDb.get(Videos.STATUS) != VideoStatus.PENDING.value && videoInDb.get(Videos.STATUS) != VideoStatus.UPLOADING.value) {
                throw Exception("Video status doesn't match.")
            }

            val chunkInDb = DBHelpers.getChunk(chunkId, id)

            val videoFilenameInDb = DBHelpers.getVideoFilename(id)

            FSHelpers.saveVideoChunkV2(chunkInDb, videoFilenameInDb, chunk)

            DBHelpers.deleteChunk(chunkId, id)
            val chunksLeft = DBHelpers.getAllChunks(id)
            if (chunksLeft.size == 0) {
                DBHelpers.updateVideoSatus(id, VideoStatus.PROCESSING)
                addToProcessingQueue(id.toString())
            } else {
                DBHelpers.updateVideoSatus(id, VideoStatus.UPLOADING)
            }
        }

        suspend fun getThumbnail(id: String) = response<ByteArray> {
            FSHelpers.getThumbnail(id)
        }

        suspend fun getAllVideosAdmin() = response<List<DBHelpers.Companion.AdminVideos>> {
            DBHelpers.getAllVideosAdmin()
        }

        suspend fun getVideoAdmin(id: String) = response<DBHelpers.Companion.AdminVideo> {
            DBHelpers.getVideoAdmin(UUID.fromString(id))
        }

        suspend fun deleteVideoAdmin(id: String) = response {
            DBHelpers.updateVideoSatus(UUID.fromString(id), VideoStatus.DELETING)
        }
    }

}