package com.leilao.backend.notifications.infrastructure.telegram

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Primary
@ConditionalOnProperty(name = ["app.telegram.provider"], havingValue = "fake", matchIfMissing = true)
class FakeTelegramGateway : TelegramGateway {

    private val log = LoggerFactory.getLogger(FakeTelegramGateway::class.java)

    override fun sendMessage(chatId: Long, text: String): String {
        val fakeId = "fake-msg-${UUID.randomUUID()}"
        log.info("[FAKE Telegram] Mensagem para chatId={}: {}", chatId, text)
        return fakeId
    }

    override fun sendWinnerMessage(chatId: Long, payload: WinnerTelegramPayload): String {
        val fakeId = "fake-msg-${UUID.randomUUID()}"
        log.info(
            "[FAKE Telegram] Vencedor chatId={} | Leilão: {} | Valor: R$ {} | PIX: {} | msgId: {}",
            chatId,
            payload.auctionTitle,
            payload.winningAmount,
            payload.sellerPixKey ?: "não informado",
            fakeId
        )
        return fakeId
    }

    override fun sendPasswordResetCode(chatId: Long, code: String) {
        log.info("[FAKE Telegram] *** RESET DE SENHA *** chatId={} | Código: {}", chatId, code)
    }

    override fun sendPaymentDeclaredMessage(chatId: Long, payload: PaymentDeclaredTelegramPayload): String {
        val fakeId = "fake-msg-${UUID.randomUUID()}"
        log.info(
            "[FAKE Telegram] Pagamento declarado para vendedor chatId={} | Leilão: {} | Valor: R$ {} | msgId: {}",
            chatId,
            payload.auctionTitle,
            payload.amount,
            fakeId
        )
        return fakeId
    }

    override fun sendPaymentConfirmedMessage(chatId: Long, payload: PaymentConfirmedTelegramPayload): String {
        val fakeId = "fake-msg-${UUID.randomUUID()}"
        log.info(
            "[FAKE Telegram] Pagamento confirmado para vencedor chatId={} | Leilão: {} | Valor: R$ {} | msgId: {}",
            chatId,
            payload.auctionTitle,
            payload.amount,
            fakeId
        )
        return fakeId
    }
}
