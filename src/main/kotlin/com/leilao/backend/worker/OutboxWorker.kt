package com.leilao.backend.worker

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.notifications.application.SendWinnerNotificationUseCase
import com.leilao.backend.notifications.application.WinnerNotificationCommand
import com.leilao.backend.worker.outbox.OutboxEvent
import com.leilao.backend.worker.outbox.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Worker que processa eventos da tabela outbox_events.
 *
 * Fluxo:
 * 1. Busca eventos PENDING com available_at <= agora (com lock)
 * 2. Marca como PROCESSING dentro da transação
 * 3. Executa handler por tipo de evento
 * 4. Marca como PROCESSED ou FAILED
 *
 * Evento suportado: AUCTION_FINISHED_WITH_WINNER
 */
@Component
@ConditionalOnProperty(name = ["app.worker.outbox.enabled"], havingValue = "true", matchIfMissing = true)
class OutboxWorker(
    private val outboxEventRepository: OutboxEventRepository,
    private val sendWinnerNotificationUseCase: SendWinnerNotificationUseCase,
    private val auctionRepository: AuctionRepository,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(OutboxWorker::class.java)

    @Scheduled(cron = "\${app.worker.outbox.cron:*/30 * * * * *}")
    @Transactional
    fun processPendingEvents() {
        val now = Instant.now()
        val events = outboxEventRepository.findPendingEvents(now)

        if (events.isEmpty()) return

        log.info("OutboxWorker: processando {} eventos pendentes", events.size)

        events.forEach { event ->
            processEvent(event)
        }
    }

    private fun processEvent(event: OutboxEvent) {
        event.markProcessing()
        outboxEventRepository.save(event)

        try {
            when (event.eventType) {
                "AUCTION_FINISHED_WITH_WINNER" -> handleAuctionFinishedWithWinner(event)
                else -> log.warn("OutboxWorker: tipo de evento desconhecido: {}", event.eventType)
            }

            event.markProcessed()
            outboxEventRepository.save(event)
            log.info("OutboxWorker: evento {} processado com sucesso", event.id)

        } catch (ex: Exception) {
            log.error("OutboxWorker: falha ao processar evento {}: {}", event.id, ex.message, ex)
            event.markFailed(ex.message ?: "Erro desconhecido")
            outboxEventRepository.save(event)
        }
    }

    private fun handleAuctionFinishedWithWinner(event: OutboxEvent) {
        val payload = objectMapper.readValue(event.payloadJson, Map::class.java)

        val auctionId = UUID.fromString(payload["auctionId"] as String)
        val winnerUserId = UUID.fromString(payload["winnerUserId"] as String)
        val finalAmount = payload["finalAmount"] as Int

        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { IllegalStateException("Leilão $auctionId não encontrado") }

        sendWinnerNotificationUseCase.execute(
            WinnerNotificationCommand(
                auctionId = auctionId,
                winnerUserId = winnerUserId,
                auctionTitle = auction.title,
                finalAmount = finalAmount
            )
        )
    }
}
