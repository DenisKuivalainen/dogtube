package com.example.src.utils

import ioOperationWithErrorHandling

class Subscription {
    companion object {
        suspend fun subscribe(username: String, cardNumber: String, expiry: String, cvv: String, name: String) =ioOperationWithErrorHandling("Cannot perform a subscription.") {
            // TODO: add stripe functionality
        }
    }
}