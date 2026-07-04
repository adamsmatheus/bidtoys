package com.leilao.backend.auth.application

import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.shared.exception.ConflictException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class RequestWhatsAppVerificationUseCase(
    private val whatsAppVerificationStore: WhatsAppVerificationStore,
    private val evolutionGateway: EvolutionGateway,
    private val userRepository: UserRepository
) {

    fun execute(phoneNumber: String): WhatsAppVerificationResponse {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw ConflictException(
                "Este número de telefone já está vinculado a outra conta",
                "PHONE_ALREADY_EXISTS"
            )
        }

        val code = String.format("%06d", Random.nextInt(1_000_000))
        val token = whatsAppVerificationStore.create(phoneNumber, code)
        evolutionGateway.sendVerificationCode(phoneNumber, code)

        return WhatsAppVerificationResponse(token = token)
    }
}

data class WhatsAppVerificationResponse(val token: String)
