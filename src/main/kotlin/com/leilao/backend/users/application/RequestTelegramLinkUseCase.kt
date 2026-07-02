package com.leilao.backend.users.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

data class TelegramLinkResponse(val token: String, val deepLink: String)

@Service
class RequestTelegramLinkUseCase(
    private val telegramLinkStore: TelegramLinkStore,
    @Value("\${app.telegram.bot-username}") private val botUsername: String
) {
    fun execute(userId: UUID): TelegramLinkResponse {
        val token = telegramLinkStore.create(userId)
        val deepLink = "https://t.me/$botUsername?start=link_$token"
        return TelegramLinkResponse(token = token, deepLink = deepLink)
    }
}
