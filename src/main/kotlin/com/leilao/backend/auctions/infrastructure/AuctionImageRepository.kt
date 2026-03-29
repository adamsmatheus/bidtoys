package com.leilao.backend.auctions.infrastructure

import com.leilao.backend.auctions.domain.AuctionImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuctionImageRepository : JpaRepository<AuctionImage, UUID> {
    fun findByAuction_IdOrderByPositionAsc(auctionId: UUID): List<AuctionImage>
    fun findByAuction_IdInOrderByPositionAsc(auctionIds: List<UUID>): List<AuctionImage>
}
