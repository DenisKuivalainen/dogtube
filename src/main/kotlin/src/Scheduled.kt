package com.example.src

import DBHelpers
import com.example.src.utils.FSHelpers
import io.ktor.server.application.*
import ioOperation
import kotlinx.coroutines.*

suspend fun videosCleanup() = ioOperation {
    try {
        val videosToDelete = DBHelpers.getVideosToDelete()
        FSHelpers.deleteVideos(videosToDelete)
    } catch (e: Exception) {
        println("Video cleanup failed: ${e.message}")
    }
}

suspend fun sessionsCleanup() = ioOperation {
    try {
        DBHelpers.deleteOldSessions()
    } catch (e: Exception) {
        println("Sessions cleanup failed: ${e.message}")
    }
}

fun Application.scheduledTask() {
    environment.monitor.subscribe(ApplicationStarted) {
        val job = launch(Dispatchers.Default) {
            while (isActive) {
                listOf(
                    async { videosCleanup() },
                    async { sessionsCleanup() },
                    async { delay(60 * 60 * 1000) }).awaitAll()
            }
        }

        environment.monitor.subscribe(ApplicationStopped) {
            job.cancel()
        }
    }
}