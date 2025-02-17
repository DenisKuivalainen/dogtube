package com.example

import com.example.src.Users
import com.example.src.utils.JWT
import dev.forst.ktor.apikey.apiKey
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import ioOperation
import kotlinx.serialization.Serializable


private val dotenv = Dotenv.load()

@Serializable
data class UserSession(val name: String)

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 1800
            cookie.httpOnly = true
            cookie.secure = false
        }
    }
}

fun Application.configureSecurity() {
    install(Authentication) {
        session<UserSession>("user_session") {
            validate { session ->
                println("Validating session ${session.name}")
                val res = Users.validateSession(session.name)
                val resData = res.data
                if (resData != null) {
                    UserSessionPrincipal(username = resData, session = session.name)
                } else {
                    println(res.error)
                    null
                }
            }

            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

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