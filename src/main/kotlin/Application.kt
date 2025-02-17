package com.example

import io.ktor.server.application.*
import com.example.src.initVideoProcessingQueue
import com.example.src.scheduledTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSessions()
    configureSecurity()
    configureRouting()
    scheduledTask()
    initVideoProcessingQueue()
}
