package com.leilao.backend.notifications.infrastructure.whatsapp

import com.fasterxml.jackson.databind.ObjectMapper
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
    @Value("\${app.whatsapp.twilio.from}") private val from: String,
    @Value("\${app.whatsapp.twilio.verification-template-sid}") private val verificationTemplateSid: String,
    @Value("\${app.whatsapp.twilio.password-reset-template-sid}") private val passwordResetTemplateSid: String,
    @Value("\${app.whatsapp.twilio.outbid-template-sid}") private val outbidTemplateSid: String,
    @Value("\${app.whatsapp.twilio.winner-template-sid}") private val winnerTemplateSid: String,
    @Value("\${app.whatsapp.twilio.payment-declared-template-sid}") private val paymentDeclaredTemplateSid: String,
    @Value("\${app.whatsapp.twilio.payment-confirmed-template-sid}") private val paymentConfirmedTemplateSid: String
) : EvolutionGateway {

    private val log = LoggerFactory.getLogger(TwilioWhatsAppGateway::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    override fun sendVerificationCode(phoneNumber: String, code: String) {
        sendTemplate(phoneNumber, verificationTemplateSid, mapOf("1" to code))
        log.info("[Twilio] Código de verificação enviado via template para número={}", phoneNumber)
    }

    override fun sendPasswordResetCode(phoneNumber: String, code: String) {
        sendTemplate(phoneNumber, passwordResetTemplateSid, mapOf("1" to code))
        log.info("[Twilio] Código de reset de senha enviado para número={}", phoneNumber)
    }

    override fun sendOutbidNotification(phoneNumber: String, name: String, auctionTitle: String, newAmount: String) {
        sendTemplate(phoneNumber, outbidTemplateSid, mapOf("1" to name, "2" to auctionTitle, "3" to newAmount))
        log.info("[Twilio] Notificação de lance superado enviada para número={}", phoneNumber)
    }

    override fun sendWinnerNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String, pixKey: String?) {
        sendTemplate(
            phoneNumber, winnerTemplateSid,
            mapOf("1" to name, "2" to auctionTitle, "3" to amount, "4" to (pixKey ?: "Não informada"))
        )
        log.info("[Twilio] Notificação de vencedor enviada para número={}", phoneNumber)
    }

    override fun sendPaymentDeclaredNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String) {
        sendTemplate(phoneNumber, paymentDeclaredTemplateSid, mapOf("1" to name, "2" to auctionTitle, "3" to amount))
        log.info("[Twilio] Notificação de pagamento declarado enviada para número={}", phoneNumber)
    }

    override fun sendPaymentConfirmedNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String) {
        sendTemplate(phoneNumber, paymentConfirmedTemplateSid, mapOf("1" to name, "2" to auctionTitle, "3" to amount))
        log.info("[Twilio] Notificação de pagamento confirmado enviada para número={}", phoneNumber)
    }

    override fun sendMessage(phoneNumber: String, text: String) {
        send(phoneNumber, text)
    }

    private fun sendTemplate(phoneNumber: String, templateSid: String, variables: Map<String, String>) {
        val to = normalizeToE164(phoneNumber)
        val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"

        val credentials = Base64.getEncoder().encodeToString("$accountSid:$authToken".toByteArray())
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $credentials")
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("From", "whatsapp:$from")
            add("To", "whatsapp:$to")
            add("ContentSid", templateSid)
            add("ContentVariables", objectMapper.writeValueAsString(variables))
        }

        try {
            restTemplate.postForObject(url, HttpEntity(body, headers), Map::class.java)
        } catch (e: HttpClientErrorException) {
            throw EvolutionSendException(
                "Erro ao enviar template Twilio [$templateSid] [${e.statusCode}]: ${e.responseBodyAsString}", e
            )
        }
    }

    private fun send(phoneNumber: String, text: String) {
        val to = normalizeToE164(phoneNumber)
        val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"

        val credentials = Base64.getEncoder().encodeToString("$accountSid:$authToken".toByteArray())
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $credentials")
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("From", "whatsapp:$from")
            add("To", "whatsapp:$to")
            add("Body", text)
        }

        try {
            restTemplate.postForObject(url, HttpEntity(body, headers), Map::class.java)
        } catch (e: HttpClientErrorException) {
            throw EvolutionSendException(
                "Erro ao enviar mensagem via Twilio [${e.statusCode}]: ${e.responseBodyAsString}", e
            )
        }
    }

    private fun normalizeToE164(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        val withCountry = when {
            digits.startsWith("55") && digits.length >= 12 -> digits
            else -> "55$digits"
        }
        return "+$withCountry"
    }
}
