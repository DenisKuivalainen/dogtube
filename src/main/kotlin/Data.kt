package com.example

import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

data class AppPrincipal(val key: String) : Principal
data class UserPrincipal(val username: String) : Principal
data class UserSessionPrincipal(val username: String, val session: String) : Principal

@Serializable
data class AdminUserCredentials(val username: String, val password: String)

@Serializable
data class PostVideoRequest(val name: String, val isPremium: Boolean, val bufferSize: Int, val extension: String)

data class CreateUserRequest(val username: String, val name: String, val password: String) {
    init {
        require(isValidPassword(password)) { "Password does not meet complexity requirements." }
    }

    companion object {
        private fun isValidPassword(password: String): Boolean {
            val hasUpperCase = password.any { it.isUpperCase() }
            val hasLowerCase = password.any { it.isLowerCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecialChar = password.any { "!@#$%^&*()-_=+[]{}".contains(it) }

            return password.length >= 8 && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
        }
    }
}

@Serializable
data class LoginUserRequest(val username: String, val password: String)

@Serializable
data class EditVideoRequest(val name: String?, val isPremium: Boolean?)

@Serializable
data class SubscriptionRequest(
    val cardNumber: String,  // Must pass Luhn check
    val expiry: String,      // Format MMYY
    val cvv: String,         // 3 or 4 digits
    val name: String         // Non-empty
) {
    init {
        require(cardNumber.matches(Regex("\\d{13,19}")) && isValidLuhn(cardNumber)) { "Invalid card number" }
        require(expiry.matches(Regex("^(0[1-9]|1[0-2])[0-9]{2}$"))) { "Invalid expiry format (MMYY)" }
        require(cvv.matches(Regex("^\\d{3,4}$"))) { "Invalid CVV (3-4 digits)" }
        require(name.isNotBlank()) { "Name cannot be empty" }
    }

    private fun isValidLuhn(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}

@Serializable
data class PostMessageRequest(val message: String)