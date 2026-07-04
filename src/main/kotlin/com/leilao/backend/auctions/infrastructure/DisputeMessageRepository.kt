package com.leilao.backend.auctions.infrastructure

import com.leilao.backend.auctions.domain.DisputeMessage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DisputeMessageRepository : JpaRepository<DisputeMessage, UUID> {
    fun findByAuction_IdOrderByCreatedAtAsc(auctionId: UUID): List<DisputeMessage>
}
