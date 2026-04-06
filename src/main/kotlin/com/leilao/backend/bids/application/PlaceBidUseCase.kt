package com.leilao.backend.bids.application

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.bids.api.dto.PlaceBidRequest
import com.leilao.backend.bids.domain.Bid
import com.leilao.backend.bids.infrastructure.BidRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PlaceBidResult(val bid: Bid, val auction: Auction)

@Service
class PlaceBidUseCase(
    private val auctionRepository: AuctionRepository,
    private val bidRepository: BidRepository,
    private val userRepository: UserRepository
) {

    private val log = LoggerFactory.getLogger(PlaceBidUseCase::class.java)

    /**
     * Fluxo principal:
     * 1. Idempotência: verifica requestId duplicado
     * 2. Carrega o leilão com PESSIMISTIC WRITE lock (SELECT FOR UPDATE)
     * 3. Valida: status ativo, não é o dono, valor >= mínimo
     * 4. Salva o bid
     * 5. Atualiza auction (currentPrice, leadingBid, possível prorrogação)
     * 6. Commit da transação
     *
     * O lock garante que dois lances simultâneos com o mesmo valor
     * sejam resolvidos pelo timestamp de persistência (quem commita primeiro).
     */
    @Transactional
    fun execute(auctionId: UUID, request: PlaceBidRequest, bidderId: UUID): PlaceBidResult {
        // Idempotência: se já existe um bid com esse requestId, retorna ele
        if (request.requestId != null) {
            val existing = bidRepository.findByRequestId(request.requestId)
            if (existing.isPresent) {
                log.debug("Lance idempotente encontrado para requestId {}", request.requestId)
                val existingBid = existing.get()
                val existingAuction = auctionRepository.findById(existingBid.auctionId)
                    .orElseThrow { NotFoundException("Leilão não encontrado") }
                return PlaceBidResult(existingBid, existingAuction)
            }
        }

        // Lock pessimista para garantir consistência em concorrência
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.status.isActive()) {
            throw InvalidStateException("Leilão não está ativo", "AUCTION_NOT_ACTIVE")
        }

        if (auction.hasExpired()) {
            throw InvalidStateException("Leilão já encerrou", "AUCTION_EXPIRED")
        }

        if (auction.isOwnedBy(bidderId)) {
            throw ForbiddenException(
                "O dono do leilão não pode dar lance no próprio item",
                "OWNER_CANNOT_BID"
            )
        }

        val minimumAccepted = auction.nextMinimumBid()
        if (request.amount < minimumAccepted) {
            throw BusinessException(
                "Lance mínimo aceito é R$ $minimumAccepted (atual R$ ${auction.currentPriceAmount} + incremento R$ ${auction.minIncrementAmount})",
                "BID_BELOW_MINIMUM",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

        val increment = auction.minIncrementAmount
        if ((request.amount - auction.currentPriceAmount) % increment != 0) {
            throw BusinessException(
                "O lance deve ser um múltiplo de R$ $increment. Próximos valores válidos: R$ $minimumAccepted, R$ ${minimumAccepted + increment}...",
                "BID_NOT_MULTIPLE_OF_INCREMENT",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

        val bidder = userRepository.findById(bidderId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val bid = bidRepository.save(
            Bid(
                auction = auction,
                bidder = bidder,
                amount = request.amount,
                requestId = request.requestId
            )
        )

        // Atualiza auction: preço atual, leading bid, e possível prorrogação
        auction.receiveNewBid(bid.id, bid.amount)
        auctionRepository.save(auction)

        log.info("Lance R$ {} registrado no leilão {} por usuário {}", bid.amount, auctionId, bidderId)

        return PlaceBidResult(bid, auction)
    }
}
