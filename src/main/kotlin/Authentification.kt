package com.example

import com.example.src.utils.JWT
import dev.forst.ktor.apikey.apiKey
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import ioOperation


private val dotenv = Dotenv.load()


fun Application.configureSecurity() {
    install(Authentication) {
        apiKey("admin_api_key") {
            headerName = "x-api-key"

            validate { credentials ->
                if (credentials == dotenv["API_KEY"]) {
                    AppPrincipal(credentials)
                } else {
                    null
                }
            }

            challenge { call ->
                call.respondText("Invalid or missing API key.", status = HttpStatusCode.Unauthorized)
            }
        }

        jwt("admin_jwt") {
            verifier(JWT.getJwtVerifier())

            validate { credential ->
                val username = ioOperation { JWT.getUsernameFromCredential(credential) }
                if (username != null) {
                    UserPrincipal(username)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing JWT.")
            }
        }
    }
}