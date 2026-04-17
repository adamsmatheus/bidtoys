package com.leilao.backend.auctions.infrastructure

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.domain.ShipmentStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface AuctionRepository : JpaRepository<Auction, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    fun findByIdWithLock(id: UUID): Optional<Auction>

    fun findByStatus(status: AuctionStatus, pageable: Pageable): Page<Auction>

    @Query("""
        SELECT a FROM Auction a
        WHERE a.status = 'ACTIVE'
        AND a.endsAt <= :now
    """)
    fun findExpiredActiveAuctions(now: Instant): List<Auction>

    fun findBySellerId(sellerId: UUID, pageable: Pageable): Page<Auction>

    fun findBySellerIdAndStatus(sellerId: UUID, status: AuctionStatus, pageable: Pageable): Page<Auction>

    fun findBySellerIdAndStatusIn(sellerId: UUID, statuses: Collection<AuctionStatus>, pageable: Pageable): Page<Auction>

    fun findByStatusIn(statuses: Collection<AuctionStatus>, pageable: Pageable): Page<Auction>

    fun existsBySellerIdAndStatusNotIn(sellerId: UUID, statuses: Collection<AuctionStatus>): Boolean

    fun findByWinnerUserId(winnerUserId: UUID, pageable: Pageable): Page<Auction>

    fun findByWinnerUserIdAndStatus(winnerUserId: UUID, status: AuctionStatus, pageable: Pageable): Page<Auction>

    fun findByWinnerUserIdAndStatusIn(winnerUserId: UUID, statuses: Collection<AuctionStatus>, pageable: Pageable): Page<Auction>

    fun findBySellerIdAndWinnerUserIdIsNotNull(sellerId: UUID): List<Auction>

    fun findBySellerIdAndShipmentStatus(sellerId: UUID, shipmentStatus: ShipmentStatus, pageable: Pageable): Page<Auction>

    fun findByWinnerUserIdAndHoldShipmentTrue(winnerUserId: UUID, pageable: Pageable): Page<Auction>
}
