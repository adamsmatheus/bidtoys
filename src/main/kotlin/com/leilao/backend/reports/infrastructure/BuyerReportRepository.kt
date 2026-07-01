package com.leilao.backend.reports.infrastructure

import com.leilao.backend.reports.domain.BuyerReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BuyerReportRepository : JpaRepository<BuyerReport, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<BuyerReport>
}
