package com.leilao.backend.notifications.infrastructure.whatsapp

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.Base64

@Component
@ConditionalOnProperty(name = ["app.whatsapp.provider"], havingValue = "twilio")
class TwilioWhatsAppGateway(
    @Value("\${app.whatsapp.twilio.account-sid}") private val accountSid: String,
    @Value("\${app.whatsapp.twilio.auth-token}") private val authToken: String,
    @Value("\${app.whatsapp.twilio.from-number}") private val fromNumber: String,
    @Value("\${app.whatsapp.twilio.sms-from-number}") private val smsFromNumber: String
) : WhatsAppGateway {

    private val log = LoggerFactory.getLogger(TwilioWhatsAppGateway::class.java)
    private val restTemplate = RestTemplate()
    private val apiUrl get() = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"

    override fun sendVerificationCode(phoneNumber: String, code: String) {
        val body = "Seu codigo de verificacao Bid Toys e: $code\n\nNao compartilhe com ninguem."
        sendSms(phoneNumber, body)
        log.info("[Twilio SMS] Codigo de verificacao enviado para {}", phoneNumber)
    }

    override fun sendPasswordResetCode(phoneNumber: String, code: String) {
        val body = "Seu codigo para redefinir a senha da Bid Toys e: $code\n\nValido por 10 minutos. Nao compartilhe com ninguem."
        sendSms(phoneNumber, body)
        log.info("[Twilio SMS] Codigo de recuperacao de senha enviado para {}", phoneNumber)
    }

    override fun sendWinnerMessage(phoneNumber: String, winnerMessage: WinnerMessagePayload): String {
        val valor = formatAmount(winnerMessage.winningAmount)
        val pix = winnerMessage.sellerPixKey ?: "nao informado"
        val body = "Parabens, ${winnerMessage.recipientName}!\n\n" +
            "Voce venceu o leilao: *${winnerMessage.auctionTitle}*\n" +
            "Valor: R$ $valor\n" +
            "Chave PIX do vendedor: $pix\n\n" +
            "Acesse a Bid Toys para confirmar o pagamento."
        return send(phoneNumber, body).also {
            log.info("[Twilio] Notificacao de vitoria enviada para {} | sid={}", phoneNumber, it)
        }
    }

    override fun sendPaymentDeclaredMessage(phoneNumber: String, payload: PaymentDeclaredMessagePayload): String {
        val valor = formatAmount(payload.amount)
        val body = "Ola, ${payload.sellerName}!\n\n" +
            "O vencedor declarou o pagamento do leilao: *${payload.auctionTitle}*\n" +
            "Valor: R$ $valor\n\n" +
            "Acesse a Bid Toys para confirmar o recebimento."
        return send(phoneNumber, body).also {
            log.info("[Twilio] Pagamento declarado enviado para {} | sid={}", phoneNumber, it)
        }
    }

    override fun sendPaymentConfirmedMessage(phoneNumber: String, payload: PaymentConfirmedMessagePayload): String {
        val body = "Ola, ${payload.winnerName}!\n\n" +
            "O vendedor confirmou o pagamento do leilao: *${payload.auctionTitle}*\n\n" +
            "Seu pedido esta sendo preparado para envio."
        return send(phoneNumber, body).also {
            log.info("[Twilio] Pagamento confirmado enviado para {} | sid={}", phoneNumber, it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun sendSms(phoneNumber: String, message: String): String {
        val credentials = Base64.getEncoder().encodeToString("$accountSid:$authToken".toByteArray())
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $credentials")
        }
        val form = LinkedMultiValueMap<String, String>().apply {
            add("From", smsFromNumber)
            add("To", normalize(phoneNumber))
            add("Body", message)
        }
        try {
            val response = restTemplate.postForObject(
                apiUrl,
                HttpEntity(form, headers),
                Map::class.java
            ) ?: throw WhatsAppSendException("Resposta nula da API do Twilio SMS")
            return response["sid"]?.toString()
                ?: throw WhatsAppSendException("SID ausente na resposta SMS: $response")
        } catch (e: HttpClientErrorException) {
            throw WhatsAppSendException("Erro na API do Twilio SMS [${e.statusCode}]: ${e.responseBodyAsString}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun send(phoneNumber: String, message: String): String {
        val credentials = Base64.getEncoder().encodeToString("$accountSid:$authToken".toByteArray())
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $credentials")
        }
        val form = LinkedMultiValueMap<String, String>().apply {
            add("From", "whatsapp:${normalize(fromNumber)}")
            add("To", "whatsapp:${normalize(phoneNumber)}")
            add("Body", message)
        }
        try {
            val response = restTemplate.postForObject(
                apiUrl,
                HttpEntity(form, headers),
                Map::class.java
            ) ?: throw WhatsAppSendException("Resposta nula da API do Twilio")

            return response["sid"]?.toString()
                ?: throw WhatsAppSendException("SID ausente na resposta: $response")
        } catch (e: HttpClientErrorException) {
            throw WhatsAppSendException("Erro na API do Twilio [${e.statusCode}]: ${e.responseBodyAsString}", e)
        }
    }

    private fun normalize(phoneNumber: String): String {
        val digits = phoneNumber.replace(Regex("[^0-9+]"), "")
        return if (digits.startsWith("+")) digits else "+$digits"
    }

    private fun formatAmount(cents: Int): String =
        String.format("%.2f", cents.toDouble()).replace('.', ',')
}
