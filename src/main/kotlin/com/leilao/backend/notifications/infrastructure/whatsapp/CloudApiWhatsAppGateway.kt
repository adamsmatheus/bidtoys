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
@ConditionalOnProperty(name = ["app.whatsapp.enabled"], havingValue = "true")
class CloudApiWhatsAppGateway(
    @Value("\${app.whatsapp.token}") private val token: String,
    @Value("\${app.whatsapp.phone-id}") private val phoneId: String
) : WhatsAppGateway {

    private val log = LoggerFactory.getLogger(CloudApiWhatsAppGateway::class.java)
    private val restTemplate = RestTemplate()
    private val apiUrl get() = "https://graph.facebook.com/v20.0/$phoneId/messages"

    override fun sendVerificationCode(phoneNumber: String, code: String) {
        val body = mapOf(
            "messaging_product" to "whatsapp",
            "to" to normalize(phoneNumber),
            "type" to "template",
            "template" to mapOf(
                "name" to "verificacao_cadastro",
                "language" to mapOf("code" to "pt_BR"),
                "components" to listOf(
                    mapOf("type" to "body", "parameters" to listOf(textParam(code))),
                    mapOf("type" to "button", "sub_type" to "url", "index" to "0",
                        "parameters" to listOf(textParam(code)))
                )
            )
        )
        post(body)
        log.info("[WhatsApp] Código de verificação enviado para {}", phoneNumber)
    }

    override fun sendPasswordResetCode(phoneNumber: String, code: String) {
        val body = mapOf(
            "messaging_product" to "whatsapp",
            "to" to normalize(phoneNumber),
            "type" to "template",
            "template" to mapOf(
                "name" to "recuperacao_senha",
                "language" to mapOf("code" to "pt_BR"),
                "components" to listOf(
                    mapOf("type" to "body", "parameters" to listOf(textParam(code))),
                    mapOf("type" to "button", "sub_type" to "url", "index" to "0",
                        "parameters" to listOf(textParam(code)))
                )
            )
        )
        post(body)
        log.info("[WhatsApp] Código de recuperação de senha enviado para {}", phoneNumber)
    }

    override fun sendWinnerMessage(phoneNumber: String, winnerMessage: WinnerMessagePayload): String {
        val body = utilityTemplate(
            to = normalize(phoneNumber),
            templateName = "leilao_ganho",
            params = listOf(
                textParam(winnerMessage.recipientName),
                textParam(winnerMessage.auctionTitle),
                textParam(formatAmount(winnerMessage.winningAmount)),
                textParam(winnerMessage.sellerPixKey ?: "não informado")
            )
        )
        return post(body).also {
            log.info("[WhatsApp] Notificação de vitória enviada para {} | msgId={}", phoneNumber, it)
        }
    }

    override fun sendPaymentDeclaredMessage(phoneNumber: String, payload: PaymentDeclaredMessagePayload): String {
        val body = utilityTemplate(
            to = normalize(phoneNumber),
            templateName = "pagamento_declarado",
            params = listOf(
                textParam(payload.sellerName),
                textParam(payload.auctionTitle),
                textParam(formatAmount(payload.amount))
            )
        )
        return post(body).also {
            log.info("[WhatsApp] Notificação de pagamento declarado enviada para {} | msgId={}", phoneNumber, it)
        }
    }

    override fun sendPaymentConfirmedMessage(phoneNumber: String, payload: PaymentConfirmedMessagePayload): String {
        val body = utilityTemplate(
            to = normalize(phoneNumber),
            templateName = "pagamento_confirmado",
            params = listOf(
                textParam(payload.winnerName),
                textParam(payload.auctionTitle)
            )
        )
        return post(body).also {
            log.info("[WhatsApp] Notificação de pagamento confirmado enviada para {} | msgId={}", phoneNumber, it)
        }
    }

    private fun utilityTemplate(to: String, templateName: String, params: List<Map<String, String>>) = mapOf(
        "messaging_product" to "whatsapp",
        "to" to to,
        "type" to "template",
        "template" to mapOf(
            "name" to templateName,
            "language" to mapOf("code" to "pt_BR"),
            "components" to listOf(mapOf("type" to "body", "parameters" to params))
        )
    )

    private fun textParam(value: String) = mapOf("type" to "text", "text" to value)

    @Suppress("UNCHECKED_CAST")
    private fun post(body: Map<String, Any>): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }
        try {
            val response = restTemplate.postForObject(
                apiUrl,
                HttpEntity(body, headers),
                Map::class.java
            ) ?: throw WhatsAppSendException("Resposta nula da API do WhatsApp")

            val messages = response["messages"] as? List<Map<String, Any>>
            return messages?.firstOrNull()?.get("id")?.toString()
                ?: throw WhatsAppSendException("ID da mensagem ausente na resposta: $response")
        } catch (e: HttpClientErrorException) {
            throw WhatsAppSendException("Erro na API do WhatsApp [${e.statusCode}]: ${e.responseBodyAsString}", e)
        }
    }

    private fun normalize(phoneNumber: String): String {
        val digits = phoneNumber.replace(Regex("[^0-9+]"), "")
        return if (digits.startsWith("+")) digits.substring(1) else digits
    }

    private fun formatAmount(cents: Int): String =
        String.format("%.2f", cents.toDouble()).replace('.', ',')
}
