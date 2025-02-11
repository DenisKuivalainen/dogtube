package com.example.src

import AdminUsers
import DBHelpers
import VideoStatus
import Videos
import com.example.src.utils.Crypt
import com.example.src.utils.FSHelpers
import com.example.src.utils.JWT
import com.example.src.utils.processImage
import ioOperation
import kotlinx.serialization.Serializable
import java.util.*

class Admin {
    companion object {
        @Serializable
        data class AdminUser(val username: String)

        suspend fun getAdminUser(username: String): AdminUser? = ioOperation {
            try {
                val usernameInDb =
                    DBHelpers.getAdminUser(username)?.get(AdminUsers.USERNAME) ?: throw Exception("User not exists.")

                AdminUser(usernameInDb)
            } catch (e: Exception) {
                null
            }
        }

        suspend fun createAdminUser(username: String, password: String): Boolean = ioOperation {
            try {
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

        suspend fun authAdminUser(username: String, password: String): JwtResponse? = ioOperation {
            try {
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
        ): DBHelpers.Companion.VideoCreationResponse? = ioOperation {
            try {
                val res = DBHelpers.createVideoV2(name, isPremium, bufferSize, extension)

                if (res != null) {
                    FSHelpers.createMockFile(res.id, extension, bufferSize)
                }

                res
            } catch (e: Exception) {
                println(e)
                null
            }
        }

            suspend fun uploadVideoChunkV2(_id: String, _chunkId: String, chunk: ByteArray): Boolean =ioOperation {
                 try {
                    val id = UUID.fromString(_id)
                    val chunkId = UUID.fromString(_chunkId)

                    val videoInDb = DBHelpers.getVideo(id)
                    if (videoInDb?.get(Videos.STATUS) != VideoStatus.PENDING.value && videoInDb?.get(Videos.STATUS) != VideoStatus.UPLOADING.value) {
                        throw Exception("Video status doesn't match.")
                    }

                    val chunkInDb = DBHelpers.getChunk(chunkId, id) ?:throw Exception("Chunk does not exists.")

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

            suspend fun getThumbnail(id: String): ByteArray? =ioOperation {
                 FSHelpers.getThumbnail(id)
            }

            suspend fun getAllVideosAdmin(): List<DBHelpers.Companion.AdminVideos> = ioOperation {
                 DBHelpers.getAllVideosAdmin()
            }

        suspend fun getVideoAdmin(id: String): DBHelpers.Companion.AdminVideo? = ioOperation {
            DBHelpers.getVideoAdmin(UUID.fromString(id))
        }

        suspend fun deleteVideoAdmin(id: String): Boolean = ioOperation {
            try {
                DBHelpers.updateVideoSatus(UUID.fromString(id), VideoStatus.DELETING)
                true
            } catch(e: Exception) {
                false
            }
        }
    }

}