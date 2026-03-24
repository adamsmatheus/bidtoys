package com.leilao.backend.auth.application

import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppGateway
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class SendWhatsAppCodeUseCase(
    private val whatsAppGateway: WhatsAppGateway,
    private val verificationStore: WhatsAppVerificationStore
) {

    fun execute(phoneNumber: String) {
        val code = String.format("%06d", Random.nextInt(0, 1_000_000))
        verificationStore.save(phoneNumber, code)
        whatsAppGateway.sendVerificationCode(phoneNumber, code)
    }
}
