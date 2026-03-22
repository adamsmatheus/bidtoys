package com.leilao.backend.bids.infrastructure

import com.leilao.backend.bids.domain.Bid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface BidRepository : JpaRepository<Bid, UUID> {
    fun countByAuctionId(auctionId: UUID): Long
    fun findByAuctionIdOrderByCreatedAtDesc(auctionId: UUID, pageable: Pageable): Page<Bid>
    fun findByRequestId(requestId: String): Optional<Bid>
}
