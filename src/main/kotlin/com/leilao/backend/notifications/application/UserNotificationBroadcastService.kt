package com.leilao.backend.notifications.application

import com.leilao.backend.auctions.api.dto.UserNotificationMessage
import com.leilao.backend.auctions.domain.ShipmentStatus
import com.leilao.backend.notifications.domain.InAppNotification
import com.leilao.backend.notifications.infrastructure.InAppNotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserNotificationBroadcastService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val inAppNotificationRepository: InAppNotificationRepository
) {

    private val log = LoggerFactory.getLogger(UserNotificationBroadcastService::class.java)

    fun notifyAuctionWon(userId: UUID, auctionId: UUID, auctionTitle: String, finalAmount: Int) {
        send(
            userId = userId,
            auctionId = auctionId,
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
            userId = sellerId,
            auctionId = auctionId,
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
            userId = winnerId,
            auctionId = auctionId,
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
            userId = winnerId,
            auctionId = auctionId,
            UserNotificationMessage(
                type = "PAYMENT_DISPUTED",
                title = "Pagamento contestado",
                message = "O vendedor não identificou seu pagamento no leilão \"$auctionTitle\". Entre em contato com o suporte.",
                auctionId = auctionId.toString()
            )
        )
    }

    fun notifyDeliveryRequested(sellerId: UUID, auctionId: UUID, auctionTitle: String, buyerName: String) {
        send(
            userId = sellerId,
            auctionId = auctionId,
            UserNotificationMessage(
                type = "DELIVERY_REQUESTED",
                title = "Entrega solicitada",
                message = "O comprador \"$buyerName\" solicitou a entrega do produto do leilão \"$auctionTitle\".",
                auctionId = auctionId.toString()
            )
        )
    }

    fun notifyShipmentStatusChanged(
        winnerId: UUID,
        auctionId: UUID,
        auctionTitle: String,
        newStatus: ShipmentStatus,
        trackingCode: String?
    ) {
        val (title, message) = when (newStatus) {
            ShipmentStatus.PREPARING ->
                "Produto em preparação" to
                    "O vendedor está preparando o envio do seu produto do leilão \"$auctionTitle\"."
            ShipmentStatus.SHIPPED -> {
                val tracking = if (trackingCode != null) " Código de rastreio: $trackingCode." else ""
                "Produto enviado" to
                    "O seu produto do leilão \"$auctionTitle\" foi enviado.$tracking"
            }
            else -> return
        }
        send(
            userId = winnerId,
            auctionId = auctionId,
            UserNotificationMessage(
                type = "SHIPMENT_STATUS_CHANGED",
                title = title,
                message = message,
                auctionId = auctionId.toString()
            )
        )
    }

    private fun send(userId: UUID, auctionId: UUID, message: UserNotificationMessage) {
        val saved = inAppNotificationRepository.save(
            InAppNotification(
                userId = userId,
                type = message.type,
                title = message.title,
                message = message.message,
                auctionId = auctionId
            )
        )

        val messageWithId = message.copy(id = saved.id.toString(), createdAt = saved.createdAt)
        val destination = "/topic/users/$userId/notifications"
        messagingTemplate.convertAndSend(destination, messageWithId)
        log.debug("Notificação [{}] persistida e enviada para usuário {}", message.type, userId)
    }

    private fun formatAmount(cents: Int): String =
        String.format("%.2f", cents.toDouble()).replace('.', ',')
}
