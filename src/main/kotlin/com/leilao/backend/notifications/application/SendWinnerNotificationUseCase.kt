package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.admin.domain.AdminAlert
import com.leilao.backend.admin.domain.AdminAlertType
import com.leilao.backend.admin.infrastructure.AdminAlertRepository
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppSendException
import com.leilao.backend.notifications.infrastructure.whatsapp.WinnerMessagePayload
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.companies.infrastructure.CompanyRepository
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class WinnerNotificationCommand(
    val auctionId: UUID,
    val winnerUserId: UUID,
    val auctionTitle: String,
    val finalAmount: Int
)

@Service
class SendWinnerNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val adminAlertRepository: AdminAlertRepository,
    private val userRepository: UserRepository,
    private val auctionRepository: AuctionRepository,
    private val companyRepository: CompanyRepository,
    private val whatsAppGateway: WhatsAppGateway,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(SendWinnerNotificationUseCase::class.java)

    @Transactional
    fun execute(command: WinnerNotificationCommand) {
        val winner = userRepository.findById(command.winnerUserId).orElse(null)
            ?: run {
                log.error("Vencedor {} não encontrado para leilão {}", command.winnerUserId, command.auctionId)
                createAdminAlert(command, "Vencedor não encontrado no banco de dados")
                return
            }

        val payloadJson = objectMapper.writeValueAsString(command)

        val notification = notificationRepository.save(
            Notification(
                userId = command.winnerUserId,
                auctionId = command.auctionId,
                type = NotificationType.WINNER_NOTIFICATION,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = payloadJson
            )
        )

        if (!winner.whatsappEnabled) {
            log.warn("Vencedor {} não tem WhatsApp habilitado", command.winnerUserId)
            notification.markFailed("WhatsApp não habilitado")
            notificationRepository.save(notification)
            createAdminAlert(command, "Vencedor sem WhatsApp habilitado: ${winner.email}")
            return
        }

        val sellerPixKey = auctionRepository.findById(command.auctionId)
            .map { auction -> companyRepository.findByUserId(auction.seller.id).orElse(null)?.pixKey }
            .orElse(null)

        try {
            val messagePayload = WinnerMessagePayload(
                recipientName = winner.name,
                auctionTitle = command.auctionTitle,
                winningAmount = command.finalAmount,
                auctionId = command.auctionId.toString(),
                sellerPixKey = sellerPixKey
            )

            val providerMessageId = whatsAppGateway.sendWinnerMessage(winner.phoneNumber, messagePayload)

            notification.markSent(providerMessageId)
            notificationRepository.save(notification)

            log.info("Notificação de vencedor enviada com sucesso para leilão {}", command.auctionId)

        } catch (ex: WhatsAppSendException) {
            log.error("Falha ao enviar WhatsApp para leilão {}: {}", command.auctionId, ex.message)

            notification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(notification)

            createAdminAlert(command, "Falha ao enviar WhatsApp: ${ex.message}")
        }

        // IMPORTANTE: o leilão continua encerrado independentemente do resultado da notificação
    }

    private fun createAdminAlert(command: WinnerNotificationCommand, message: String) {
        adminAlertRepository.save(
            AdminAlert(
                type = AdminAlertType.WHATSAPP_FAILED,
                auctionId = command.auctionId,
                message = message
            )
        )
        log.warn("Alerta administrativo criado para leilão {}: {}", command.auctionId, message)
    }
}
