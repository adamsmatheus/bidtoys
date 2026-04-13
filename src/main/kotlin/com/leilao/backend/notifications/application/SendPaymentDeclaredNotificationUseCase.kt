package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.whatsapp.PaymentDeclaredMessagePayload
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppSendException
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PaymentDeclaredNotificationCommand(
    val auctionId: UUID,
    val sellerId: UUID,
    val winnerUserId: UUID,
    val auctionTitle: String,
    val amount: Int
)

@Service
class SendPaymentDeclaredNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val whatsAppGateway: WhatsAppGateway,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(SendPaymentDeclaredNotificationUseCase::class.java)

    @Transactional
    fun execute(command: PaymentDeclaredNotificationCommand) {
        val seller = userRepository.findById(command.sellerId).orElse(null)
            ?: run {
                log.error("Vendedor {} não encontrado para leilão {}", command.sellerId, command.auctionId)
                return
            }

        val notification = notificationRepository.save(
            Notification(
                userId = command.sellerId,
                auctionId = command.auctionId,
                type = NotificationType.PAYMENT_DECLARED,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = objectMapper.writeValueAsString(command)
            )
        )

        if (!seller.whatsappEnabled) {
            log.warn("Vendedor {} não tem WhatsApp habilitado", command.sellerId)
            notification.markFailed("WhatsApp não habilitado")
            notificationRepository.save(notification)
            return
        }

        try {
            val providerMessageId = whatsAppGateway.sendPaymentDeclaredMessage(
                seller.phoneNumber,
                PaymentDeclaredMessagePayload(
                    sellerName = seller.name,
                    auctionTitle = command.auctionTitle,
                    amount = command.amount,
                    auctionId = command.auctionId.toString()
                )
            )
            notification.markSent(providerMessageId)
            notificationRepository.save(notification)
            log.info("Notificação de pagamento declarado enviada ao vendedor {} para leilão {}", command.sellerId, command.auctionId)
        } catch (ex: WhatsAppSendException) {
            log.error("Falha ao notificar vendedor {} sobre pagamento: {}", command.sellerId, ex.message)
            notification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(notification)
        }
    }
}
