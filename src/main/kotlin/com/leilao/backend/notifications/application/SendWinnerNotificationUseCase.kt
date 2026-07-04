package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.admin.domain.AdminAlert
import com.leilao.backend.admin.domain.AdminAlertType
import com.leilao.backend.admin.infrastructure.AdminAlertRepository
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.telegram.TelegramGateway
import com.leilao.backend.notifications.infrastructure.telegram.TelegramSendException
import com.leilao.backend.notifications.infrastructure.telegram.WinnerTelegramPayload
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionSendException
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
    private val telegramGateway: TelegramGateway,
    private val whatsAppGateway: EvolutionGateway,
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

        val sellerPixKey = auctionRepository.findById(command.auctionId)
            .map { auction -> companyRepository.findByUserId(auction.seller.id).orElse(null)?.pixKey }
            .orElse(null)

        val formattedAmount = formatBRL(command.finalAmount)

        // --- WhatsApp (best-effort) ---
        val whatsAppNotification = notificationRepository.save(
            Notification(
                userId = command.winnerUserId,
                auctionId = command.auctionId,
                type = NotificationType.WINNER_NOTIFICATION,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = payloadJson
            )
        )
        try {
            whatsAppGateway.sendWinnerNotification(
                phoneNumber = winner.phoneNumber,
                name = winner.name,
                auctionTitle = command.auctionTitle,
                amount = formattedAmount,
                pixKey = sellerPixKey
            )
            whatsAppNotification.markSent()
            notificationRepository.save(whatsAppNotification)
            log.info("Notificação WhatsApp de vencedor enviada para leilão {}", command.auctionId)
        } catch (ex: EvolutionSendException) {
            log.error("Falha ao enviar WhatsApp para vencedor do leilão {}: {}", command.auctionId, ex.message)
            whatsAppNotification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(whatsAppNotification)
        }

        // --- Telegram (best-effort) ---
        if (winner.telegramChatId == null) {
            log.info("Vencedor {} não tem Telegram conectado, pulando", command.winnerUserId)
            return
        }

        val telegramNotification = notificationRepository.save(
            Notification(
                userId = command.winnerUserId,
                auctionId = command.auctionId,
                type = NotificationType.WINNER_NOTIFICATION,
                channel = NotificationChannel.TELEGRAM,
                payloadJson = payloadJson
            )
        )
        try {
            val providerMessageId = telegramGateway.sendWinnerMessage(
                winner.telegramChatId!!,
                WinnerTelegramPayload(
                    recipientName = winner.name,
                    auctionTitle = command.auctionTitle,
                    winningAmount = command.finalAmount,
                    sellerPixKey = sellerPixKey
                )
            )
            telegramNotification.markSent(providerMessageId)
            notificationRepository.save(telegramNotification)
            log.info("Notificação Telegram de vencedor enviada para leilão {}", command.auctionId)
        } catch (ex: TelegramSendException) {
            log.error("Falha ao enviar Telegram para leilão {}: {}", command.auctionId, ex.message)
            telegramNotification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(telegramNotification)
            createAdminAlert(command, "Falha ao enviar Telegram: ${ex.message}")
        }
    }

    private fun createAdminAlert(command: WinnerNotificationCommand, message: String) {
        adminAlertRepository.save(
            AdminAlert(
                type = AdminAlertType.TELEGRAM_FAILED,
                auctionId = command.auctionId,
                message = message
            )
        )
        log.warn("Alerta administrativo criado para leilão {}: {}", command.auctionId, message)
    }

    private fun formatBRL(amount: Int): String =
        String.format("%.2f", amount.toDouble()).replace(".", ",")
}
