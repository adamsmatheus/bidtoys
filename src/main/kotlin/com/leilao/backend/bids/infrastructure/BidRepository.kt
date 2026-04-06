package com.leilao.backend.bids.infrastructure

import com.leilao.backend.bids.domain.Bid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface BidRepository : JpaRepository<Bid, UUID> {
    fun countByAuction_Id(auctionId: UUID): Long

    @Query(
        value = "SELECT b FROM Bid b JOIN FETCH b.bidder WHERE b.auction.id = :auctionId ORDER BY b.createdAt DESC",
        countQuery = "SELECT COUNT(b) FROM Bid b WHERE b.auction.id = :auctionId"
    )
    fun findByAuction_IdOrderByCreatedAtDesc(auctionId: UUID, pageable: Pageable): Page<Bid>

    fun findByRequestId(requestId: String): Optional<Bid>
}
