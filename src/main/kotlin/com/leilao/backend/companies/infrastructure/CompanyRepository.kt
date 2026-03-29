package com.leilao.backend.companies.infrastructure

import com.leilao.backend.companies.domain.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CompanyRepository : JpaRepository<Company, UUID> {

    fun findByUserId(userId: UUID): Optional<Company>

    fun findByUserIdIn(userIds: List<UUID>): List<Company>

    @Query("""
        SELECT DISTINCT c FROM Company c
        WHERE EXISTS (
            SELECT a FROM Auction a
            WHERE a.seller.id = c.user.id
            AND a.status = 'ACTIVE'
        )
    """)
    fun findCompaniesWithActiveAuctions(): List<Company>
}
