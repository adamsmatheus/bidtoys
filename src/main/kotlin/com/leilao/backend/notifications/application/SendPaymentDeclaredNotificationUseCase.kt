package com.leilao.backend.notifications.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.telegram.PaymentDeclaredTelegramPayload
import com.leilao.backend.notifications.infrastructure.telegram.TelegramGateway
import com.leilao.backend.notifications.infrastructure.telegram.TelegramSendException
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionSendException
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
    private val whatsAppGateway: EvolutionGateway,
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

        val payloadJson = objectMapper.writeValueAsString(command)
        val formattedAmount = formatBRL(command.amount)

        // --- WhatsApp (best-effort) ---
        val whatsAppNotification = notificationRepository.save(
            Notification(
                userId = command.sellerId,
                auctionId = command.auctionId,
                type = NotificationType.PAYMENT_DECLARED,
                channel = NotificationChannel.WHATSAPP,
                payloadJson = payloadJson
            )
        )
        try {
            whatsAppGateway.sendPaymentDeclaredNotification(
                phoneNumber = seller.phoneNumber,
                name = seller.name,
                auctionTitle = command.auctionTitle,
                amount = formattedAmount
            )
            whatsAppNotification.markSent()
            notificationRepository.save(whatsAppNotification)
            log.info("Notificação WhatsApp de pagamento declarado enviada ao vendedor {} para leilão {}", command.sellerId, command.auctionId)
        } catch (ex: EvolutionSendException) {
            log.error("Falha ao enviar WhatsApp de pagamento declarado para vendedor {}: {}", command.sellerId, ex.message)
            whatsAppNotification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(whatsAppNotification)
        }

        // --- Telegram (best-effort) ---
        if (seller.telegramChatId == null) {
            log.info("Vendedor {} não tem Telegram conectado, pulando", command.sellerId)
            return
        }

        val telegramNotification = notificationRepository.save(
            Notification(
                userId = command.sellerId,
                auctionId = command.auctionId,
                type = NotificationType.PAYMENT_DECLARED,
                channel = NotificationChannel.TELEGRAM,
                payloadJson = payloadJson
            )
        )
        try {
            val providerMessageId = telegramGateway.sendPaymentDeclaredMessage(
                seller.telegramChatId!!,
                PaymentDeclaredTelegramPayload(
                    sellerName = seller.name,
                    auctionTitle = command.auctionTitle,
                    amount = command.amount
                )
            )
            telegramNotification.markSent(providerMessageId)
            notificationRepository.save(telegramNotification)
            log.info("Notificação Telegram de pagamento declarado enviada ao vendedor {} para leilão {}", command.sellerId, command.auctionId)
        } catch (ex: TelegramSendException) {
            log.error("Falha ao notificar vendedor {} sobre pagamento via Telegram: {}", command.sellerId, ex.message)
            telegramNotification.markFailed(ex.message ?: "Erro desconhecido")
            notificationRepository.save(telegramNotification)
        }
    }

    private fun formatBRL(amount: Int): String =
        String.format("%.2f", amount.toDouble()).replace(".", ",")
}
