package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        helloWorld()
        adminUser()
        adminVideos()
        user()
        videos()
        subscription()

        staticResources("/static", "static")
    }
}
