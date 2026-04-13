package com.leilao.backend.notifications.application

import com.leilao.backend.auctions.api.dto.UserNotificationMessage
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserNotificationBroadcastService(
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(UserNotificationBroadcastService::class.java)

    fun notifyAuctionWon(userId: UUID, auctionId: UUID, auctionTitle: String, finalAmount: Int) {
        send(
            userId,
            UserNotificationMessage(
                type = "AUCTION_WON",
                title = "Você arrematou!",
                message = "Parabéns! Você venceu o leilão \"$auctionTitle\" por R$ ${formatAmount(finalAmount)}. Realize o pagamento via PIX.",
                auctionId = auctionId.toString()
            )
        )
    }

    fun notifyPaymentDeclared(sellerId: UUID, auctionId: UUID, auctionTitle: String, amount: Int) {
        send(
            sellerId,
            UserNotificationMessage(
                type = "PAYMENT_DECLARED",
                title = "Pagamento declarado",
                message = "O vencedor do leilão \"$auctionTitle\" declarou ter realizado o pagamento de R$ ${formatAmount(amount)} via PIX. Verifique seu extrato.",
                auctionId = auctionId.toString()
            )
        )
    }

    fun notifyPaymentConfirmed(winnerId: UUID, auctionId: UUID, auctionTitle: String) {
        send(
            winnerId,
            UserNotificationMessage(
                type = "PAYMENT_CONFIRMED",
                title = "Pagamento confirmado",
                message = "O vendedor confirmou o recebimento do seu pagamento no leilão \"$auctionTitle\". Entre em contato para combinar a entrega.",
                auctionId = auctionId.toString()
            )
        )
    }

    fun notifyPaymentDisputed(winnerId: UUID, auctionId: UUID, auctionTitle: String) {
        send(
            winnerId,
            UserNotificationMessage(
                type = "PAYMENT_DISPUTED",
                title = "Pagamento contestado",
                message = "O vendedor não identificou seu pagamento no leilão \"$auctionTitle\". Entre em contato com o suporte.",
                auctionId = auctionId.toString()
            )
        )
    }

    private fun send(userId: UUID, message: UserNotificationMessage) {
        val destination = "/topic/users/$userId/notifications"
        messagingTemplate.convertAndSend(destination, message)
        log.debug("Notificação [{}] enviada para usuário {}", message.type, userId)
    }

    private fun formatAmount(cents: Int): String =
        String.format("%.2f", cents.toDouble()).replace('.', ',')
}
