package com.leilao.backend.reports.api.dto

import com.leilao.backend.reports.domain.AuctionReport
import com.leilao.backend.reports.domain.AuctionReportStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SubmitAuctionReportRequest(
    @field:NotBlank(message = "Motivo é obrigatório")
    @field:Size(max = 2000, message = "Motivo deve ter no máximo 2000 caracteres")
    val reason: String
)

data class AuctionReportResponse(
    val id: UUID,
    val auctionId: UUID,
    val auctionTitle: String,
    val reporterId: UUID,
    val reporterName: String,
    val reason: String,
    val status: AuctionReportStatus,
    val resolvedAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(report: AuctionReport) = AuctionReportResponse(
            id = report.id,
            auctionId = report.auctionId,
            auctionTitle = report.auctionTitle,
            reporterId = report.reporterId,
            reporterName = report.reporterName,
            reason = report.reason,
            status = report.status,
            resolvedAt = report.resolvedAt,
            createdAt = report.createdAt
        )
    }
}
