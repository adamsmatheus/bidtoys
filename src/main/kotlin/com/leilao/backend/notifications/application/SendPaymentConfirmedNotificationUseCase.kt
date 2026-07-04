package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.telegram.PaymentConfirmedTelegramPayload
import com.leilao.backend.notifications.infrastructure.telegram.TelegramGateway
import com.leilao.backend.notifications.infrastructure.telegram.TelegramSendException
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionSendException
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
    private val telegramGateway: TelegramGateway,
    private val whatsAppGateway: EvolutionGateway,
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

        val payloadJson = objectMapper.writeValueAsString(command)
        val formattedAmount = formatBRL(command.amount)

        // --- WhatsApp (best-effort) ---
        val whatsAppNotification = notificationRepository.save(
            Notification(
                userId = command.winnerUserId,
                auctionId = command.auctionId,
                type = NotificationType.PAYMENT_CONFIRMED,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = payloadJson
            )
        )
        try {
            whatsAppGateway.sendPaymentConfirmedNotification(
                phoneNumber = winner.phoneNumber,
                name = winner.name,
                auctionTitle = command.auctionTitle,
                amount = formattedAmount
            )
            whatsAppNotification.markSent()
            notificationRepository.save(whatsAppNotification)
            log.info("Notificação WhatsApp de pagamento confirmado enviada ao vencedor {} para leilão {}", command.winnerUserId, command.auctionId)
        } catch (ex: EvolutionSendException) {
            log.error("Falha ao enviar WhatsApp de pagamento confirmado para vencedor {}: {}", command.winnerUserId, ex.message)
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
                type = NotificationType.PAYMENT_CONFIRMED,
                channel = NotificationChannel.TELEGRAM,
                payloadJson = payloadJson
            )
        )
        try {
            val providerMessageId = telegramGateway.sendPaymentConfirmedMessage(
                winner.telegramChatId!!,
                PaymentConfirmedTelegramPayload(
                    winnerName = winner.name,
                    auctionTitle = command.auctionTitle,
                    amount = command.amount
                )
            )
            telegramNotification.markSent(providerMessageId)
            notificationRepository.save(telegramNotification)
            log.info("Notificação Telegram de pagamento confirmado enviada ao vencedor {} para leilão {}", command.winnerUserId, command.auctionId)
        } catch (ex: TelegramSendException) {
            log.error("Falha ao notificar vencedor {} sobre confirmação via Telegram: {}", command.winnerUserId, ex.message)
            telegramNotification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(telegramNotification)
        }
    }

    private fun formatBRL(amount: Int): String =
        String.format("%.2f", amount.toDouble()).replace(".", ",")
}
