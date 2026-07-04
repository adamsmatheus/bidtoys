package com.leilao.backend.notifications.infrastructure.whatsapp

interface EvolutionGateway {

    fun sendVerificationCode(phoneNumber: String, code: String)

    fun sendPasswordResetCode(phoneNumber: String, code: String)

    fun sendOutbidNotification(phoneNumber: String, name: String, auctionTitle: String, newAmount: String)

    fun sendWinnerNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String, pixKey: String?)

    fun sendPaymentDeclaredNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String)

    fun sendPaymentConfirmedNotification(phoneNumber: String, name: String, auctionTitle: String, amount: String)

    fun sendMessage(phoneNumber: String, text: String)
}

class EvolutionSendException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
