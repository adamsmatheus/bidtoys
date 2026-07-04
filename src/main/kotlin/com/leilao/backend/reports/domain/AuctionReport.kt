package com.leilao.backend.reports.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auction_reports")
class AuctionReport(

    @Column(name = "auction_id", nullable = false)
    val auctionId: UUID,

    @Column(name = "auction_title", nullable = false, length = 255)
    val auctionTitle: String,

    @Column(name = "reporter_id", nullable = false)
    val reporterId: UUID,

    @Column(name = "reporter_name", nullable = false, length = 150)
    val reporterName: String,

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    val reason: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: AuctionReportStatus = AuctionReportStatus.PENDING,

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

) : BaseEntity()
