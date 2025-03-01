package com.example.src

import DBHelpers
import VideoStatus
import com.example.src.utils.*
import io.ktor.http.content.*
import kotlinx.serialization.Serializable
import java.io.InputStream
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
            } catch (_: Exception) {
                // If user was not found

                val hashedPassword = Crypt.hashPassword(password)
                return@response DBHelpers.createAdminUser(username, hashedPassword)
            }

            throw Exception("User already exists.")
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
            if (videoInDb.status != VideoStatus.PENDING.value && videoInDb.status != VideoStatus.UPLOADING.value) {
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

        suspend fun uploadVideoV2(multipartData: MultiPartData) = response {
            val id = UUID.randomUUID()

            var name: String? = null
            var isPremium: Boolean = false
            var extension: String? = null

            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name" -> name = part.value
                            "isPremium" -> isPremium = part.value.toBooleanStrictOrNull() ?: false
                        }
                    }

                    is PartData.FileItem -> {
                        if(extension == null) {
                            extension = part.originalFileName?.split(".")?.getOrNull(1)
                        }
                        extension = part.originalFileName ?: "uploaded_file"
                        FSHelpers.writeVideoAsStream("$id.$extension", part.streamProvider())
                    }

                    else -> Unit
                }
                part.dispose()
            }
            if (name == null || extension == null) throw Exception("required payload can not be null.")

            DBHelpers.createVideoV2(id, extension!!, name!!, isPremium)
            addToProcessingQueue(id.toString())
        }

        suspend fun editVideo(id: String,  name: String?, isPremium: Boolean?) = response {
            DBHelpers.editVideoAdmin(UUID.fromString(id), name, isPremium)
        }

        suspend fun getVideoStatisticsForPastMonth(videoId: String) = response {
            DBHelpers.getPastMonthVideoStatisticsAdmin(UUID.fromString(videoId))
        }
    }

}