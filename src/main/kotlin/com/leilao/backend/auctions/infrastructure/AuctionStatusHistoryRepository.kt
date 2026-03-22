package com.leilao.backend.auctions.infrastructure

import com.leilao.backend.auctions.domain.AuctionStatusHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuctionStatusHistoryRepository : JpaRepository<AuctionStatusHistory, UUID> {
    fun findByAuctionIdOrderByCreatedAtAsc(auctionId: UUID): List<AuctionStatusHistory>
}
