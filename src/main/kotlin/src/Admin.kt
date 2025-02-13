package com.example.src

import DBHelpers
import VideoStatus
import com.example.src.utils.*
import ioOperation
import kotlinx.serialization.Serializable
import java.util.*

class Admin {
    companion object {
        @Serializable
        data class AdminUser(val username: String)

        suspend fun getAdminUser(username: String): AdminUser?  {
            return try {
                val usernameInDb =
                    DBHelpers.getAdminUser(username)?.get(AdminUsers.USERNAME) ?: throw Exception("User not exists.")

                AdminUser(usernameInDb)
            } catch (e: Exception) {
                null
            }
        }

        suspend fun createAdminUser(username: String, password: String): Boolean  {
            return try {
                val userInDb = DBHelpers.getAdminUser(username) ?: throw Exception("User not exists.")

                val hashedPassword = Crypt.hashPassword(password)
                DBHelpers.putAdminUser(username, hashedPassword)

                true
            } catch (e: Exception) {
                false
            }
        }

        @Serializable
        data class JwtResponse(val jwt: String)

        suspend fun authAdminUser(username: String, password: String): JwtResponse?  {
            return try {
                val userInDb = DBHelpers.getAdminUser(username) ?: throw Exception("User not exists.")

                val hashedPassword = userInDb.get(AdminUsers.PASSWORD)

                if (!Crypt.checkPassword(password, hashedPassword ?: "")) {
                    throw Exception("Password do not match")
                }

                JwtResponse(JWT.create(userInDb.get(AdminUsers.USERNAME)))
            } catch (e: Exception) {
                null
            }
        }

        suspend fun createVideo(
            name: String, isPremium: Boolean, bufferSize: Int, extension: String
        ): DBHelpers.Companion.VideoCreationResponse?  {
            return try {
                val res = DBHelpers.createVideo(name, isPremium, bufferSize, extension)

                if (res != null) {
                    FSHelpers.createMockFile(res.id, extension, bufferSize)
                }

                res
            } catch (e: Exception) {
                println(e)
                null
            }
        }

        suspend fun uploadVideoChunk(_id: String, _chunkId: String, chunk: ByteArray): Boolean  {
            return try {
                val id = UUID.fromString(_id)
                val chunkId = UUID.fromString(_chunkId)

                val videoInDb = DBHelpers.getVideo(id)
                if (videoInDb?.get(Videos.STATUS) != VideoStatus.PENDING.value && videoInDb?.get(Videos.STATUS) != VideoStatus.UPLOADING.value) {
                    throw Exception("Video status doesn't match.")
                }

                val chunkInDb = DBHelpers.getChunk(chunkId, id) ?: throw Exception("Chunk does not exists.")

                val videoFilenameInDb = DBHelpers.getVideoFilename(id) ?: throw Exception("Cannot get video filename.")

                val saveResult = FSHelpers.saveVideoChunkV2(chunkInDb, videoFilenameInDb, chunk)
                if (saveResult) {
                    DBHelpers.deleteChunk(chunkId, id)
                    val chunksLeft = DBHelpers.getAllChunks(id)
                    if (chunksLeft.size == 0) {
                        DBHelpers.updateVideoSatus(id, VideoStatus.PROCESSING)
//                        videoProcessingQueue.put(id.toString())
                        addToProcessingQueue(id.toString())
                    } else {
                        DBHelpers.updateVideoSatus(id, VideoStatus.UPLOADING)
                    }

                }

                saveResult
            } catch (e: Exception) {
                println(e)
                false
            }
        }

        suspend fun getThumbnail(id: String): ByteArray?  {
            return FSHelpers.getThumbnail(id)
        }

        suspend fun getAllVideosAdmin(): List<DBHelpers.Companion.AdminVideos>  {
            return DBHelpers.getAllVideosAdmin()
        }

        suspend fun getVideoAdmin(id: String): DBHelpers.Companion.AdminVideo?  {
            return DBHelpers.getVideoAdmin(UUID.fromString(id))
        }

        suspend fun deleteVideoAdmin(id: String): Boolean  {
            return try {
                DBHelpers.updateVideoSatus(UUID.fromString(id), VideoStatus.DELETING)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

}