package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.domain.DisputeMessage
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.DisputeMessageRepository
import com.leilao.backend.notifications.application.UserNotificationBroadcastService
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SendDisputeMessageUseCase(
    private val auctionRepository: AuctionRepository,
    private val disputeMessageRepository: DisputeMessageRepository,
    private val userRepository: UserRepository,
    private val userNotificationBroadcastService: UserNotificationBroadcastService,
    private val whatsAppGateway: EvolutionGateway
) {

    private val log = LoggerFactory.getLogger(SendDisputeMessageUseCase::class.java)

    @Transactional
    fun execute(auctionId: UUID, senderId: UUID, message: String): DisputeMessage {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (auction.status.name != "PAYMENT_DISPUTED") {
            throw InvalidStateException("Chat de disputa disponível apenas em leilões com pagamento contestado")
        }

        val isSeller = auction.seller.id == senderId
        val isWinner = auction.winnerUserId == senderId
        if (!isSeller && !isWinner) {
            throw ForbiddenException("Apenas o vendedor ou vencedor podem enviar mensagens nesta disputa")
        }

        val sender = userRepository.findById(senderId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val savedMessage = disputeMessageRepository.save(
            DisputeMessage(
                auction = auction,
                senderId = senderId,
                senderName = sender.name,
                message = message.trim()
            )
        )

        // Notificar a outra parte
        val recipientId = if (isSeller) auction.winnerUserId!! else auction.seller.id
        userNotificationBroadcastService.notifyDisputeMessage(
            recipientId = recipientId,
            auctionId = auction.id,
            auctionTitle = auction.title,
            senderName = sender.name
        )

        val recipient = userRepository.findById(recipientId).orElse(null)
        if (recipient != null) {
            try {
                val role = if (isSeller) "Vendedor" else "Comprador"
                whatsAppGateway.sendMessage(
                    phoneNumber = recipient.phoneNumber,
                    text = "💬 *$role enviou uma mensagem na disputa do leilão \"${auction.title}\"*\n\n\"${message.trim()}\"\n\nAcesse o site para responder."
                )
            } catch (ex: Exception) {
                log.warn("Falha ao enviar WhatsApp de mensagem de disputa para {}: {}", recipientId, ex.message)
            }
        }

        return savedMessage
    }
}
