package com.leilao.backend.auth.api

import com.leilao.backend.auth.application.TelegramVerificationStore
import com.leilao.backend.notifications.infrastructure.telegram.TelegramGateway
import com.leilao.backend.users.application.TelegramLinkStore
import com.leilao.backend.users.infrastructure.UserRepository
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
    private val telegramLinkStore: TelegramLinkStore,
    private val userRepository: UserRepository,
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
            val param = text.removePrefix("/start ").trim()

            if (param.startsWith("link_")) {
                val token = param.removePrefix("link_")
                val userId = telegramLinkStore.markLinked(token, chatId)
                if (userId != null) {
                    userRepository.findById(userId).ifPresent { user ->
                        user.telegramChatId = chatId
                        userRepository.save(user)
                    }
                    telegramGateway.sendMessage(chatId, "Telegram vinculado com sucesso! Voce vai receber notificacoes dos seus leiloes por aqui.")
                    log.info("[Telegram Webhook] Telegram vinculado para userId={} chatId={}", userId, chatId)
                } else {
                    telegramGateway.sendMessage(chatId, "Link invalido ou expirado. Volte ao app e solicite um novo link.")
                    log.warn("[Telegram Webhook] Token de vinculacao invalido para chatId={}", chatId)
                }
            } else {
                val token = param
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
}
