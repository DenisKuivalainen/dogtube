package com.example.src

import DBHelpers
import com.example.src.utils.FSHelpers
import io.ktor.server.application.*
import ioOperation
import kotlinx.coroutines.*

suspend fun videosCleanup() = ioOperation {
    val videosToDelete = DBHelpers.getVideosToDelete()
    FSHelpers.deleteVideos(videosToDelete)
}

fun Application.scheduledTask() {
    environment.monitor.subscribe(ApplicationStarted) {
        val job = launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    videosCleanup()
                } catch (e: Exception) {
                    println(e.message)
                }
                delay(60*60*1000)
            }
        }

        environment.monitor.subscribe(ApplicationStopped) {
            job.cancel()
        }
    }
}