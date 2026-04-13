package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.whatsapp.PaymentConfirmedMessagePayload
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppSendException
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PaymentConfirmedNotificationCommand(
    val auctionId: UUID,
    val winnerUserId: UUID,
    val sellerId: UUID,
    val auctionTitle: String,
    val amount: Int
)

@Service
class SendPaymentConfirmedNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val whatsAppGateway: WhatsAppGateway,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(SendPaymentConfirmedNotificationUseCase::class.java)

    @Transactional
    fun execute(command: PaymentConfirmedNotificationCommand) {
        val winner = userRepository.findById(command.winnerUserId).orElse(null)
            ?: run {
                log.error("Vencedor {} não encontrado para leilão {}", command.winnerUserId, command.auctionId)
                return
            }

        val notification = notificationRepository.save(
            Notification(
                userId = command.winnerUserId,
                auctionId = command.auctionId,
                type = NotificationType.PAYMENT_CONFIRMED,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = objectMapper.writeValueAsString(command)
            )
        )

        if (!winner.whatsappEnabled) {
            log.warn("Vencedor {} não tem WhatsApp habilitado", command.winnerUserId)
            notification.markFailed("WhatsApp não habilitado")
            notificationRepository.save(notification)
            return
        }

        try {
            val providerMessageId = whatsAppGateway.sendPaymentConfirmedMessage(
                winner.phoneNumber,
                PaymentConfirmedMessagePayload(
                    winnerName = winner.name,
                    auctionTitle = command.auctionTitle,
                    amount = command.amount,
                    auctionId = command.auctionId.toString()
                )
            )
            notification.markSent(providerMessageId)
            notificationRepository.save(notification)
            log.info("Notificação de pagamento confirmado enviada ao vencedor {} para leilão {}", command.winnerUserId, command.auctionId)
        } catch (ex: WhatsAppSendException) {
            log.error("Falha ao notificar vencedor {} sobre confirmação: {}", command.winnerUserId, ex.message)
            notification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(notification)
        }
    }
}
