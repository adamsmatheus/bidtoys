package com.leilao.backend.notifications.infrastructure.whatsapp

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Implementação fake do WhatsApp Gateway para desenvolvimento local e testes.
 * Substitua por uma implementação real (CloudApiWhatsAppGateway) em produção,
 * controlado por profile ou feature flag.
 */
@Component
@Primary
class FakeWhatsAppGateway : WhatsAppGateway {

    private val log = LoggerFactory.getLogger(FakeWhatsAppGateway::class.java)

    override fun sendWinnerMessage(phoneNumber: String, winnerMessage: WinnerMessagePayload): String {
        val fakeMessageId = "fake-msg-${UUID.randomUUID()}"

        log.info(
            "[FAKE WhatsApp] Enviando mensagem para {} | Leilão: {} | Vencedor: {} | Valor: R$ {} | PIX: {} | msgId: {}",
            phoneNumber,
            winnerMessage.auctionTitle,
            winnerMessage.recipientName,
            winnerMessage.winningAmount,
            winnerMessage.sellerPixKey ?: "não informado",
            fakeMessageId
        )

        // Simular falha aleatória para testes (descomente para testar o fluxo de falha):
        // if (Random.nextBoolean()) throw WhatsAppSendException("Simulated failure")

        return fakeMessageId
    }

    override fun sendVerificationCode(phoneNumber: String, code: String) {
        log.info(
            "[FAKE WhatsApp] Código de verificação para {}: {}",
            phoneNumber,
            code
        )
    }
}
