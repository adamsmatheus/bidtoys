package com.leilao.backend.auth.application

import com.leilao.backend.shared.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class VerifyWhatsAppCodeUseCase(
    private val whatsAppVerificationStore: WhatsAppVerificationStore
) {

    fun execute(token: String, code: String): WhatsAppVerifyCodeResponse {
        val verified = whatsAppVerificationStore.verify(token, code)
        if (!verified) {
            throw BusinessException(
                "Código inválido ou expirado",
                "WHATSAPP_CODE_INVALID",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }
        return WhatsAppVerifyCodeResponse(verified = true)
    }
}

data class WhatsAppVerifyCodeResponse(val verified: Boolean)
