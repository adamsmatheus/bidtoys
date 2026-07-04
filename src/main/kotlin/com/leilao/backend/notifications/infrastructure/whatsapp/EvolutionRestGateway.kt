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
        val text = """
            *Bid Toys — Verificação de celular*

            Seu código de verificação: *$code*

            Válido por 10 minutos. Não compartilhe com ninguém.
        """.trimIndent()
        send(phoneNumber, text)
        log.info("[WhatsApp] Código de verificação enviado para número={}", phoneNumber)
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
