package com.leilao.backend.reports.infrastructure

import com.leilao.backend.reports.domain.AuctionReport
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuctionReportRepository : JpaRepository<AuctionReport, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<AuctionReport>
}
