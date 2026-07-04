package com.leilao.backend.reports.application

import com.leilao.backend.reports.domain.AuctionReport
import com.leilao.backend.reports.infrastructure.AuctionReportRepository
import org.springframework.stereotype.Service

@Service
class ListAuctionReportsUseCase(
    private val auctionReportRepository: AuctionReportRepository
) {
    fun execute(): List<AuctionReport> =
        auctionReportRepository.findAllByOrderByCreatedAtDesc()
}
