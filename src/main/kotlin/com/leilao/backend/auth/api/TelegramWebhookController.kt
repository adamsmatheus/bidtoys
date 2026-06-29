package com.leilao.backend.auth.api

import com.leilao.backend.auth.application.TelegramVerificationStore
import com.leilao.backend.notifications.infrastructure.telegram.TelegramGateway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/telegram")
class TelegramWebhookController(
    private val verificationStore: TelegramVerificationStore,
    private val telegramGateway: TelegramGateway,
    @Value("\${app.telegram.webhook-secret:}") private val webhookSecret: String
) {

    private val log = LoggerFactory.getLogger(TelegramWebhookController::class.java)

    @PostMapping("/webhook")
    fun handleUpdate(
        @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) secret: String?,
        @RequestBody update: Map<String, Any>
    ): ResponseEntity<Unit> {
        if (webhookSecret.isNotBlank() && secret != webhookSecret) {
            log.warn("[Telegram Webhook] Secret inválido recebido")
            return ResponseEntity.status(401).build()
        }

        try {
            processUpdate(update)
        } catch (e: Exception) {
            log.error("[Telegram Webhook] Erro ao processar update: {}", e.message)
        }

        // Telegram exige 200 OK independente do resultado
        return ResponseEntity.ok().build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun processUpdate(update: Map<String, Any>) {
        val message = update["message"] as? Map<String, Any> ?: return
        val chat = message["chat"] as? Map<String, Any> ?: return
        val chatId = (chat["id"] as? Number)?.toLong() ?: return
        val text = message["text"] as? String ?: return

        if (text.startsWith("/start ")) {
            val token = text.removePrefix("/start ").trim()
            if (verificationStore.markVerified(token, chatId)) {
                telegramGateway.sendMessage(chatId, "Numero verificado com sucesso! Volte ao app para completar o cadastro.")
                log.info("[Telegram Webhook] Token verificado para chatId={}", chatId)
            } else {
                telegramGateway.sendMessage(chatId, "Link invalido ou expirado. Volte ao app e solicite um novo link de verificacao.")
                log.warn("[Telegram Webhook] Token invalido ou expirado para chatId={}", chatId)
            }
        }
    }
}
