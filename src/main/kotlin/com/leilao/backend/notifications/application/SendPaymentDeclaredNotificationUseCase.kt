package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.telegram.PaymentDeclaredTelegramPayload
import com.leilao.backend.notifications.infrastructure.telegram.TelegramGateway
import com.leilao.backend.notifications.infrastructure.telegram.TelegramSendException
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
    private val telegramGateway: TelegramGateway,
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
                channel = NotificationChannel.TELEGRAM,
                payloadJson = objectMapper.writeValueAsString(command)
            )
        )

        if (seller.telegramChatId == null) {
            log.warn("Vendedor {} não tem Telegram conectado", command.sellerId)
            notification.markFailed("Telegram não conectado")
            notificationRepository.save(notification)
            return
        }

        try {
            val providerMessageId = telegramGateway.sendPaymentDeclaredMessage(
                seller.telegramChatId!!,
                PaymentDeclaredTelegramPayload(
                    sellerName = seller.name,
                    auctionTitle = command.auctionTitle,
                    amount = command.amount
                )
            )
            notification.markSent(providerMessageId)
            notificationRepository.save(notification)
            log.info("Notificação de pagamento declarado enviada ao vendedor {} para leilão {}", command.sellerId, command.auctionId)
        } catch (ex: TelegramSendException) {
            log.error("Falha ao notificar vendedor {} sobre pagamento: {}", command.sellerId, ex.message)
            notification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(notification)
        }
    }
}
