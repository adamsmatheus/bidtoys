package com.leilao.backend.worker

import com.leilao.backend.auctions.application.FinishAuctionUseCase
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Worker responsável por encerrar leilões expirados.
 *
 * Estratégia de execução:
 * - Roda a cada minuto via @Scheduled (cron configurável)
 * - Busca leilões ACTIVE cujo ends_at <= agora
 * - Para cada um, executa FinishAuctionUseCase dentro de transação com lock
 * - Se a instância for escalada horizontalmente, o lock no banco evita processamento duplo
 *
 * Habilitação por profile/property:
 * - app.worker.auction-closure.enabled=true (default: true)
 * - Para rodar apenas o worker: spring.profiles.active=worker
 * - Para rodar apenas a API sem worker: desabilitar via property
 */
@Component
@ConditionalOnProperty(name = ["app.worker.auction-closure.enabled"], havingValue = "true", matchIfMissing = true)
class AuctionClosureWorker(
    private val auctionRepository: AuctionRepository,
    private val finishAuctionUseCase: FinishAuctionUseCase
) {

    private val log = LoggerFactory.getLogger(AuctionClosureWorker::class.java)

    @Scheduled(cron = "\${app.worker.auction-closure.cron:0 * * * * *}")
    fun processExpiredAuctions() {
        val now = Instant.now()
        val expired = auctionRepository.findExpiredActiveAuctions(now)

        if (expired.isEmpty()) return

        log.info("Worker: encontrados {} leilões expirados para encerrar", expired.size)

        expired.forEach { auction ->
            try {
                finishAuctionUseCase.execute(auction.id)
            } catch (ex: Exception) {
                log.error("Worker: erro ao encerrar leilão {}: {}", auction.id, ex.message, ex)
            }
        }
    }
}
