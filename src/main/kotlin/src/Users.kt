package com.example.src

import DBHelpers
import DBHelpers.Companion.createSession
import com.example.src.utils.Crypt
import io.ktor.http.*

class Users {
    companion object {
        private fun createCookie(sessionId: String) = Cookie(
            name = "user_session", value = sessionId, maxAge = 1800, httpOnly = true,
            // secure = true,
            path = "/"
        )

        suspend fun createUser(username: String, name: String, password: String) = response<Cookie> {
            try {
                DBHelpers.getUser(username)
                throw Exception("User already exists.")
            } catch (_: Exception) {
                // If user was not found

                DBHelpers.createUser(username, name, Crypt.hashPassword(password))

                createCookie(createSession(username))
            }
        }

        suspend fun loginUser(username: String, password: String) = response<Cookie> {
            val userPasswordInDb = DBHelpers.getUserPassword(username)
            if(!Crypt.checkPassword(password, userPasswordInDb)) throw Exception("User password does not match.")

            createCookie(createSession(username))
        }

        suspend fun getUserData(username: String) = response {
            DBHelpers.getUser(username)
        }
    }
}