package com.leilao.backend.notifications.infrastructure.whatsapp

/**
 * Gateway abstrato para envio de mensagens WhatsApp.
 * A implementação real será integrada via WhatsApp Cloud API / BSP.
 */
interface WhatsAppGateway {

    /**
     * Envia mensagem ao vencedor do leilão.
     * @return providerMessageId retornado pelo provider
     * @throws WhatsAppSendException em caso de falha no envio
     */
    fun sendWinnerMessage(phoneNumber: String, winnerMessage: WinnerMessagePayload): String

    /**
     * Envia código de verificação de 6 dígitos para confirmação do número de WhatsApp.
     * @throws WhatsAppSendException em caso de falha no envio
     */
    fun sendVerificationCode(phoneNumber: String, code: String)

    /**
     * Envia código de reset de senha via WhatsApp.
     * @throws WhatsAppSendException em caso de falha no envio
     */
    fun sendPasswordResetCode(phoneNumber: String, code: String)

    /**
     * Notifica o vendedor que o vencedor declarou ter realizado o pagamento.
     * @throws WhatsAppSendException em caso de falha no envio
     */
    fun sendPaymentDeclaredMessage(phoneNumber: String, payload: PaymentDeclaredMessagePayload): String

    /**
     * Notifica o vencedor que o vendedor confirmou o recebimento do pagamento.
     * @throws WhatsAppSendException em caso de falha no envio
     */
    fun sendPaymentConfirmedMessage(phoneNumber: String, payload: PaymentConfirmedMessagePayload): String
}

data class WinnerMessagePayload(
    val recipientName: String,
    val auctionTitle: String,
    val winningAmount: Int,
    val auctionId: String,
    val sellerPixKey: String?
)

data class PaymentDeclaredMessagePayload(
    val sellerName: String,
    val auctionTitle: String,
    val amount: Int,
    val auctionId: String
)

data class PaymentConfirmedMessagePayload(
    val winnerName: String,
    val auctionTitle: String,
    val amount: Int,
    val auctionId: String
)

class WhatsAppSendException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
