package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionSendException
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class OutbidNotificationCommand(
    val auctionId: UUID,
    val outbidUserId: UUID,
    val auctionTitle: String,
    val newAmount: Int
)

@Service
class SendOutbidNotificationUseCase(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val whatsAppGateway: EvolutionGateway,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(SendOutbidNotificationUseCase::class.java)

    @Transactional
    fun execute(command: OutbidNotificationCommand) {
        val user = userRepository.findById(command.outbidUserId).orElse(null)
            ?: run {
                log.warn("Usuário {} não encontrado para notificação de lance superado", command.outbidUserId)
                return
            }

        val notification = notificationRepository.save(
            Notification(
                userId = command.outbidUserId,
                auctionId = command.auctionId,
                type = NotificationType.AUCTION_OUTBID,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = objectMapper.writeValueAsString(command)
            )
        )

        val formattedAmount = formatBRL(command.newAmount)

        try {
            whatsAppGateway.sendOutbidNotification(
                phoneNumber = user.phoneNumber,
                name = user.name,
                auctionTitle = command.auctionTitle,
                newAmount = formattedAmount
            )
            notification.markSent(null)
            notificationRepository.save(notification)
            log.info("Notificação de lance superado enviada para usuário {} no leilão {}", command.outbidUserId, command.auctionId)
        } catch (ex: EvolutionSendException) {
            log.error("Falha ao enviar notificação de lance superado para usuário {}: {}", command.outbidUserId, ex.message)
            notification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(notification)
        }
    }

    private fun formatBRL(amount: Int): String =
        String.format("%.2f", amount.toDouble()).replace(".", ",")
}
