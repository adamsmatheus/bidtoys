package com.leilao.backend.reports.api.dto

import com.leilao.backend.auctions.api.dto.BuyerAuctionItem
import com.leilao.backend.reports.domain.BuyerReport
import com.leilao.backend.reports.domain.BuyerReportStatus
import com.leilao.backend.users.api.dto.AddressResponse
import java.time.Instant
import java.util.UUID

data class BuyerReportResponse(
    val id: UUID,
    val reporterId: UUID,
    val reporterName: String,
    val reportedUserId: UUID,
    val reportedUserName: String,
    val reportedUserStatus: String,
    val reason: String,
    val imageUrls: List<String>,
    val status: BuyerReportStatus,
    val resolvedAt: Instant?,
    val createdAt: Instant,
    val buyerEmail: String,
    val buyerPhone: String?,
    val buyerAddress: AddressResponse?,
    val buyerAuctions: List<BuyerAuctionItem>
) {
    companion object {
        fun from(
            report: BuyerReport,
            reportedUserStatus: String,
            buyerEmail: String,
            buyerPhone: String?,
            buyerAddress: AddressResponse?,
            buyerAuctions: List<BuyerAuctionItem>
        ) = BuyerReportResponse(
            id = report.id,
            reporterId = report.reporterId,
            reporterName = report.reporterName,
            reportedUserId = report.reportedUserId,
            reportedUserName = report.reportedUserName,
            reportedUserStatus = reportedUserStatus,
            reason = report.reason,
            imageUrls = report.imageUrls,
            status = report.status,
            resolvedAt = report.resolvedAt,
            createdAt = report.createdAt,
            buyerEmail = buyerEmail,
            buyerPhone = buyerPhone,
            buyerAddress = buyerAddress,
            buyerAuctions = buyerAuctions
        )
    }
}

data class ResolveReportRequest(
    val action: String // "DEACTIVATE" or "KEEP_ACTIVE"
)
