package com.leilao.backend.reports.application

import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.reports.domain.AuctionReport
import com.leilao.backend.reports.infrastructure.AuctionReportRepository
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SubmitAuctionReportUseCase(
    private val auctionRepository: AuctionRepository,
    private val auctionReportRepository: AuctionReportRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun execute(auctionId: UUID, reporterId: UUID, reason: String): UUID {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (auction.status.name != "PAYMENT_DISPUTED") {
            throw InvalidStateException("Leilão só pode ser reportado durante uma disputa de pagamento")
        }

        val isSeller = auction.seller.id == reporterId
        val isWinner = auction.winnerUserId == reporterId
        if (!isSeller && !isWinner) {
            throw ForbiddenException("Apenas o vendedor ou vencedor podem reportar este leilão")
        }

        val reporter = userRepository.findById(reporterId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val report = auctionReportRepository.save(
            AuctionReport(
                auctionId = auctionId,
                auctionTitle = auction.title,
                reporterId = reporterId,
                reporterName = reporter.name,
                reason = reason
            )
        )

        return report.id
    }
}
