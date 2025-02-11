package com.example.src.utils

import DBHelpers
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.auth.jwt.*
import ioOperation
import java.util.*

private val dotenv = Dotenv.load()
private val secret = dotenv["JWT_SECRET"]!!

class JWT {
    companion object {
        fun create(username: String): String {
            return JWT.create().withIssuedAt(Date()).withClaim("username", username)
                .withExpiresAt(Date(System.currentTimeMillis() + 3600000)).sign(Algorithm.HMAC256(secret))
        }

        fun getJwtVerifier(): JWTVerifier {
            return JWT.require(Algorithm.HMAC256(secret)).build()
        }

        suspend fun getUsernameFromCredential(credential: JWTCredential): String? =ioOperation {
             try {
                val username = credential.payload.getClaim("username").asString()
                if (username == "") {
                    throw Exception("Invalid username.")
                }

                DBHelpers.getAdminUser(username) ?: throw Exception("Unable to find admin user.")

                username
            } catch (e: Exception) {
                null
            }
        }
    }
}