package com.leilao.backend.notifications.infrastructure.telegram

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
@ConditionalOnProperty(name = ["app.telegram.provider"], havingValue = "bot")
class TelegramBotGateway(
    @Value("\${app.telegram.bot-token}") private val botToken: String
) : TelegramGateway {

    private val log = LoggerFactory.getLogger(TelegramBotGateway::class.java)
    private val restTemplate = RestTemplate()
    private val apiUrl get() = "https://api.telegram.org/bot$botToken/sendMessage"

    override fun sendMessage(chatId: Long, text: String): String = post(chatId, text)

    override fun sendWinnerMessage(chatId: Long, payload: WinnerTelegramPayload): String {
        val pix = payload.sellerPixKey ?: "não informado"
        val valor = formatAmount(payload.winningAmount)
        val text = """
            *Parabéns, ${payload.recipientName}!*

            Você venceu o leilão: *${payload.auctionTitle}*
            Valor: R$ $valor
            Chave PIX do vendedor: `$pix`

            Acesse a Bid Toys para confirmar o pagamento.
        """.trimIndent()
        return post(chatId, text).also {
            log.info("[Telegram] Notificação de vitória enviada para chatId={}", chatId)
        }
    }

    override fun sendPasswordResetCode(chatId: Long, code: String) {
        val text = """
            *Recuperação de senha - Bid Toys*

            Seu código: *$code*
            Válido por 15 minutos. Não compartilhe com ninguém.
        """.trimIndent()
        post(chatId, text)
        log.info("[Telegram] Código de reset de senha enviado para chatId={}", chatId)
    }

    override fun sendPaymentDeclaredMessage(chatId: Long, payload: PaymentDeclaredTelegramPayload): String {
        val valor = formatAmount(payload.amount)
        val text = """
            *Pagamento Declarado*

            Olá, ${payload.sellerName}!
            O vencedor declarou o pagamento do leilão: *${payload.auctionTitle}*
            Valor: R$ $valor

            Acesse a Bid Toys para confirmar o recebimento.
        """.trimIndent()
        return post(chatId, text).also {
            log.info("[Telegram] Notificação de pagamento declarado enviada para chatId={}", chatId)
        }
    }

    override fun sendPaymentConfirmedMessage(chatId: Long, payload: PaymentConfirmedTelegramPayload): String {
        val valor = formatAmount(payload.amount)
        val text = """
            *Pagamento Confirmado!*

            Olá, ${payload.winnerName}!
            O vendedor confirmou o pagamento do leilão: *${payload.auctionTitle}*
            Valor: R$ $valor

            Seu pedido está sendo preparado para envio.
        """.trimIndent()
        return post(chatId, text).also {
            log.info("[Telegram] Notificação de pagamento confirmado enviada para chatId={}", chatId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun post(chatId: Long, text: String): String {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = mapOf("chat_id" to chatId, "text" to text, "parse_mode" to "Markdown")
        try {
            val response = restTemplate.postForObject(
                apiUrl,
                HttpEntity(body, headers),
                Map::class.java
            ) ?: throw TelegramSendException("Resposta nula da API do Telegram")

            val result = response["result"] as? Map<String, Any>
            return result?.get("message_id")?.toString()
                ?: throw TelegramSendException("message_id ausente na resposta: $response")
        } catch (e: HttpClientErrorException) {
            throw TelegramSendException("Erro na API do Telegram [${e.statusCode}]: ${e.responseBodyAsString}", e)
        }
    }

    private fun formatAmount(cents: Int): String =
        String.format("%.2f", cents.toDouble()).replace('.', ',')
}
