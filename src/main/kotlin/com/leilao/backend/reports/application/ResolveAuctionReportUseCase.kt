package com.leilao.backend.reports.application

import com.leilao.backend.reports.domain.AuctionReportStatus
import com.leilao.backend.reports.infrastructure.AuctionReportRepository
import com.leilao.backend.shared.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ResolveAuctionReportUseCase(
    private val auctionReportRepository: AuctionReportRepository
) {

    @Transactional
    fun execute(reportId: UUID) {
        val report = auctionReportRepository.findById(reportId)
            .orElseThrow { NotFoundException("Reporte não encontrado") }

        report.status = AuctionReportStatus.RESOLVED
        report.resolvedAt = Instant.now()
        auctionReportRepository.save(report)
    }
}
