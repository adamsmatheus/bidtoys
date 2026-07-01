package com.leilao.backend.auth.application

import com.leilao.backend.shared.exception.ConflictException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RequestTelegramVerificationUseCase(
    private val verificationStore: TelegramVerificationStore,
    private val userRepository: UserRepository,
    @Value("\${app.telegram.bot-username}") private val botUsername: String
) {

    fun execute(phoneNumber: String): TelegramVerificationResponse {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw ConflictException(
                "Este número de telefone já está vinculado a outra conta",
                "PHONE_ALREADY_EXISTS"
            )
        }

        val token = verificationStore.create(phoneNumber)
        val deepLink = "https://t.me/$botUsername?start=$token"
        return TelegramVerificationResponse(token = token, deepLink = deepLink)
    }
}

data class TelegramVerificationResponse(val token: String, val deepLink: String)
