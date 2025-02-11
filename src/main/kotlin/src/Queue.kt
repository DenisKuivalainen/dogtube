package com.example.src

import DBHelpers
import com.example.src.utils.FSHelpers
import com.example.src.utils.generateThumbnail
import com.example.src.utils.resizeVideo
import ioOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.*

val semaphore = Semaphore(10)

suspend fun processVideo(_videoId: String) = ioOperation {
    semaphore.acquire()
    try{
    val videoId = UUID.fromString(_videoId)

    val srcVideoName = DBHelpers.getVideoFilename(videoId)
    val processedVideoName = "${_videoId}.mp4"

    val resizeResult = resizeVideo(srcVideoName!!, processedVideoName)
    if (!resizeResult) {
        throw Exception("Video resize error.")
    }

    val thumbnailGenerationResult = generateThumbnail(_videoId, FSHelpers.getVideoSrcFilePath(srcVideoName))
    if (!thumbnailGenerationResult) {
        throw Exception("Thumbnail generation error.")
    }

    DBHelpers.deleteVideoTemp(videoId)
    FSHelpers.deleteSrcVideo(srcVideoName)
    DBHelpers.setVideoReady(videoId)}
    finally {
        semaphore.release()
    }
}

val videoProcessingChannel = Channel<String>(Channel.UNLIMITED)
val videoProcessingScope = CoroutineScope(Dispatchers.Default)


fun initVideoProcessingQueue() {
    videoProcessingScope.launch {
        for (videoId in videoProcessingChannel) {
            processVideo(videoId)
        }
    }
}

fun addToProcessingQueue(videoId: String) {
    GlobalScope.launch {
        videoProcessingChannel.send(videoId)
    }
}