package com.leilao.backend.auth.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RequestTelegramVerificationUseCase(
    private val verificationStore: TelegramVerificationStore,
    @Value("\${app.telegram.bot-username}") private val botUsername: String
) {

    fun execute(phoneNumber: String): TelegramVerificationResponse {
        val token = verificationStore.create(phoneNumber)
        val deepLink = "https://t.me/$botUsername?start=$token"
        return TelegramVerificationResponse(token = token, deepLink = deepLink)
    }
}

data class TelegramVerificationResponse(val token: String, val deepLink: String)
