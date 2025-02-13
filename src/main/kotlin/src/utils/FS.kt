package com.example.src.utils

import DBHelpers
import ioOperation
import ioOperationWithErrorHandling
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class FS(private val directory: String) {

    init{
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    fun getFilePath(fileName: String): String {
        return File(directory, fileName).path
    }

    suspend fun saveFile(fileName: String, content: ByteArray) = ioOperationWithErrorHandling("Cannot save file $fileName.") {
        val file = File(directory, fileName)
        file.writeBytes(content)
    }

    suspend fun readFile(fileName: String): ByteArray = ioOperationWithErrorHandling("Can't read file $fileName.") {
        val file = File(directory, fileName)
        if(!file.exists()) {throw Exception()}

        file.readBytes()
    }

    suspend fun appendFile(fileName: String, content: ByteArray) =ioOperationWithErrorHandling("Cannot append file $fileName.") {
        val file = File(directory, fileName)
        if (file.exists()) {
            file.appendBytes(content)
        } else {
            saveFile(fileName, content)
        }
    }

    suspend fun deleteFile(fileName: String) = ioOperationWithErrorHandling("Cannot delete file $fileName") {
        val file = File(directory, fileName)
        file.exists() && file.delete()
    }

    suspend fun replaceBytes(filename: String, start: Int, size: Int, data: ByteArray) = ioOperationWithErrorHandling("Cannot replace bytes of file $filename.") {
        val file = File(directory, filename)
        if (!file.exists()) {
            throw Exception("File does not exist")
        }

        val fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)
        val mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, start.toLong(), size.toLong())

        mappedBuffer.clear()
        mappedBuffer.put(data)

        fileChannel.close()
    }
}

class FSHelpers {
    companion object {
        private val thumbnailDir = "thumbnail"
        private val videoUploadDir = "video_src"
        private val videoDir = "video"

        suspend fun saveThumbnail(id: String, data: ByteArray)  {
              FS(thumbnailDir).saveFile("${id}.jpg", data)
        }

        suspend fun createMockFile(id: String, extension: String, size: Int)  {
             FS(videoUploadDir).saveFile("${id}.${extension}", ByteArray(size))
        }

        suspend fun saveVideoChunkV2(chunk: DBHelpers.Companion.Chunk, fileName: String, data: ByteArray)  {
              FS(videoUploadDir).replaceBytes(fileName, chunk.start, chunk.chunkSize, data)
        }

        fun getVideoSrcFilePath(filename: String): String {
            return FS(videoUploadDir).getFilePath(filename)
        }

        fun getProcessedVideoFilePath(filename: String): String {
            return FS(videoDir).getFilePath(filename)
        }

        private val tempThumbnailFilename = "temp_thumbnail.jpg"
        fun getTempThumbnailPath(): String {
            return FS(thumbnailDir).getFilePath(tempThumbnailFilename)
        }

        suspend fun getTempThumbnailBytes(): ByteArray {
            return FS(thumbnailDir).readFile(tempThumbnailFilename)
        }

        suspend fun deleteTempThumbnail() {
            FS(thumbnailDir).deleteFile(tempThumbnailFilename)
        }

        suspend fun deleteSrcVideo(filename: String)  {
             FS(videoUploadDir).deleteFile(filename)
        }

        suspend fun getThumbnail(id: String): ByteArray  {
             val res = FS(thumbnailDir).readFile("${id}.jpg")
            res ?: throw Exception()
            return res
        }

        suspend fun deleteVideos(videos: List<DBHelpers.Companion.VideoToDelete>): Boolean  {
            return try {
                videos.forEach { video ->
                    FS(videoDir).deleteFile("${video.id}.mp4")
                    FS(thumbnailDir).deleteFile("${video.id}.jpg")
                    video.filename != null && FS(videoUploadDir).deleteFile(video.filename)
                }
                true
            } catch(e: Exception) {
                println(e)
                false
            }
        }
    }
}