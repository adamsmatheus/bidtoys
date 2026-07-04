package com.leilao.backend.notifications.infrastructure.whatsapp

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
@ConditionalOnProperty(name = ["app.whatsapp.provider"], havingValue = "evolution")
class EvolutionRestGateway(
    @Value("\${app.whatsapp.evolution.base-url}") private val baseUrl: String,
    @Value("\${app.whatsapp.evolution.api-key}") private val apiKey: String,
    @Value("\${app.whatsapp.evolution.instance}") private val instance: String
) : EvolutionGateway {

    private val log = LoggerFactory.getLogger(EvolutionRestGateway::class.java)
    private val restTemplate = RestTemplate()

    override fun sendVerificationCode(phoneNumber: String, code: String) {
        val text = "Bid Toys — seu código de verificação: *$code*\n\nVálido por 10 minutos. Não compartilhe com ninguém."
        send(phoneNumber, text)
        log.info("[Evolution WhatsApp] Código de verificação enviado para número={}", phoneNumber)
    }

    override fun sendPasswordResetCode(phoneNumber: String, code: String) {
        val text = "Bid Toys — seu código de recuperação de senha: *$code*\n\nVálido por 10 minutos. Não compartilhe com ninguém."
        send(phoneNumber, text)
        log.info("[Evolution WhatsApp] Código de reset enviado para número={}", phoneNumber)
    }

    override fun sendOutbidNotification(phoneNumber: String, name: String, auctionTitle: String, newAmount: String) {
        val text = "Olá, $name!\n\nSeu lance no leilão *$auctionTitle* foi superado.\n\nNovo lance líder: R$ $newAmount\n\nAcesse a Bid Toys para dar um novo lance!"
        send(phoneNumber, text)
        log.info("[Evolution WhatsApp] Notificação de lance superado enviada para número={}", phoneNumber)
    }

    override fun sendWinnerNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String, pixKey: String?) {
        val pixInfo = if (pixKey != null) "\nChave PIX do vendedor: $pixKey" else ""
        val text = "Parabéns, $name!\n\nVocê venceu o leilão *$auctionTitle*\nValor: R$ $amount$pixInfo\n\nAcesse a Bid Toys para confirmar o pagamento."
        send(phoneNumber, text)
        log.info("[Evolution WhatsApp] Notificação de vencedor enviada para número={}", phoneNumber)
    }

    override fun sendPaymentDeclaredNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String) {
        val text = "Olá, $name!\n\nO vencedor declarou o pagamento do leilão *$auctionTitle*\nValor: R$ $amount\n\nAcesse a Bid Toys para confirmar o recebimento."
        send(phoneNumber, text)
        log.info("[Evolution WhatsApp] Notificação de pagamento declarado enviada para número={}", phoneNumber)
    }

    override fun sendPaymentConfirmedNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String) {
        val text = "Olá, $name!\n\nO pagamento do leilão *$auctionTitle* foi confirmado.\nValor: R$ $amount\n\nObrigado por usar a Bid Toys!"
        send(phoneNumber, text)
        log.info("[Evolution WhatsApp] Notificação de pagamento confirmado enviada para número={}", phoneNumber)
    }

    override fun sendMessage(phoneNumber: String, text: String) {
        send(phoneNumber, text)
    }

    private fun send(phoneNumber: String, text: String) {
        val normalized = normalizePhone(phoneNumber)
        val url = "$baseUrl/message/sendText/$instance"
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("apikey", apiKey)
        }
        val body = mapOf("number" to normalized, "text" to text)
        try {
            restTemplate.postForObject(url, HttpEntity(body, headers), Map::class.java)
        } catch (e: HttpClientErrorException) {
            throw EvolutionSendException(
                "Erro ao enviar mensagem WhatsApp [${e.statusCode}]: ${e.responseBodyAsString}", e
            )
        }
    }

    /**
     * Normaliza número para formato Evolution API: apenas dígitos, com DDI 55 se não tiver.
     * Ex: "(11) 99999-9999" → "5511999999999"
     */
    private fun normalizePhone(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.startsWith("55") && digits.length >= 12 -> digits
            else -> "55$digits"
        }
    }
}
