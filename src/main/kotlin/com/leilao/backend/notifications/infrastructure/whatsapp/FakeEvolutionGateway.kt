package com.leilao.backend.notifications.infrastructure.whatsapp

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnProperty(name = ["app.whatsapp.provider"], havingValue = "fake", matchIfMissing = true)
class FakeEvolutionGateway : EvolutionGateway {

    private val log = LoggerFactory.getLogger(FakeEvolutionGateway::class.java)

    override fun sendVerificationCode(phoneNumber: String, code: String) {
        log.info("[FAKE WhatsApp] *** CÓDIGO DE VERIFICAÇÃO *** número={} | código={}", phoneNumber, code)
    }

    override fun sendPasswordResetCode(phoneNumber: String, code: String) {
        log.info("[FAKE WhatsApp] *** RESET DE SENHA *** número={} | código={}", phoneNumber, code)
    }

    override fun sendOutbidNotification(phoneNumber: String, name: String, auctionTitle: String, newAmount: String) {
        log.info("[FAKE WhatsApp] Lance superado: número={} | nome={} | leilão={} | novoValor={}", phoneNumber, name, auctionTitle, newAmount)
    }

    override fun sendWinnerNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String, pixKey: String?) {
        log.info("[FAKE WhatsApp] Vencedor: número={} | nome={} | leilão={} | valor={} | pix={}", phoneNumber, name, auctionTitle, amount, pixKey)
    }

    override fun sendPaymentDeclaredNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String) {
        log.info("[FAKE WhatsApp] Pagamento declarado: número={} | nome={} | leilão={} | valor={}", phoneNumber, name, auctionTitle, amount)
    }

    override fun sendPaymentConfirmedNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String) {
        log.info("[FAKE WhatsApp] Pagamento confirmado: número={} | nome={} | leilão={} | valor={}", phoneNumber, name, auctionTitle, amount)
    }

    override fun sendMessage(phoneNumber: String, text: String) {
        log.info("[FAKE WhatsApp] número={} | mensagem={}", phoneNumber, text)
    }
}
