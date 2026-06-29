package com.leilao.backend.notifications.infrastructure.telegram

interface TelegramGateway {

    fun sendMessage(chatId: Long, text: String): String

    fun sendWinnerMessage(chatId: Long, payload: WinnerTelegramPayload): String

    fun sendPasswordResetCode(chatId: Long, code: String)

    fun sendPaymentDeclaredMessage(chatId: Long, payload: PaymentDeclaredTelegramPayload): String

    fun sendPaymentConfirmedMessage(chatId: Long, payload: PaymentConfirmedTelegramPayload): String
}

data class WinnerTelegramPayload(
    val recipientName: String,
    val auctionTitle: String,
    val winningAmount: Int,
    val sellerPixKey: String?
)

data class PaymentDeclaredTelegramPayload(
    val sellerName: String,
    val auctionTitle: String,
    val amount: Int
)

data class PaymentConfirmedTelegramPayload(
    val winnerName: String,
    val auctionTitle: String,
    val amount: Int
)

class TelegramSendException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
