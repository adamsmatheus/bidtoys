package com.leilao.backend.reports.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "buyer_reports")
class BuyerReport(

    @Column(name = "reporter_id", nullable = false)
    val reporterId: UUID,

    @Column(name = "reporter_name", nullable = false, length = 150)
    val reporterName: String,

    @Column(name = "reported_user_id", nullable = false)
    val reportedUserId: UUID,

    @Column(name = "reported_user_name", nullable = false, length = 150)
    val reportedUserName: String,

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    val reason: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "buyer_report_images", joinColumns = [JoinColumn(name = "report_id")])
    @Column(name = "image_url")
    val imageUrls: MutableList<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: BuyerReportStatus = BuyerReportStatus.PENDING,

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

) : BaseEntity()
