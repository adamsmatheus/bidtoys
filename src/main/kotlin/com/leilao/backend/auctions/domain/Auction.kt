package com.leilao.backend.auctions.domain

import com.leilao.backend.shared.domain.AuditableEntity
import com.leilao.backend.users.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auctions")
class Auction(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "initial_price_amount", nullable = false)
    var initialPriceAmount: Int,

    @Column(name = "min_increment_amount", nullable = false)
    var minIncrementAmount: Int,

    @Column(name = "duration_seconds", nullable = false)
    var durationSeconds: Int

) : AuditableEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: AuctionStatus = AuctionStatus.DRAFT
        private set

    @Column(name = "current_price_amount", nullable = false)
    var currentPriceAmount: Int = initialPriceAmount
        private set

    @Column(name = "started_at")
    var startedAt: Instant? = null
        private set

    @Column(name = "ends_at")
    var endsAt: Instant? = null
        private set

    @Column(name = "approved_by_user_id")
    var approvedByUserId: UUID? = null
        private set

    @Column(name = "approved_at")
    var approvedAt: Instant? = null
        private set

    @Column(name = "rejected_by_user_id")
    var rejectedByUserId: UUID? = null
        private set

    @Column(name = "rejected_at")
    var rejectedAt: Instant? = null
        private set

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    var rejectionReason: String? = null
        private set

    @Column(name = "cancelled_by_user_id")
    var cancelledByUserId: UUID? = null
        private set

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null
        private set

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    var cancellationReason: String? = null
        private set

    @Column(name = "leading_bid_id")
    var leadingBidId: UUID? = null
        private set

    @Column(name = "winner_user_id")
    var winnerUserId: UUID? = null
        private set

    @Column(name = "finished_at")
    var finishedAt: Instant? = null
        private set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    // --- Transições de estado ---

    fun submitForApproval() {
        check(status.canSubmitForApproval()) {
            "Leilão no estado $status não pode ser enviado para aprovação"
        }
        status = AuctionStatus.PENDING_APPROVAL
    }

    fun approve(adminUserId: UUID) {
        check(status.canApproveOrReject()) {
            "Leilão no estado $status não pode ser aprovado"
        }
        status = AuctionStatus.READY_TO_START
        approvedByUserId = adminUserId
        approvedAt = Instant.now()
        rejectedByUserId = null
        rejectedAt = null
        rejectionReason = null
    }

    fun reject(adminUserId: UUID, reason: String) {
        check(status.canApproveOrReject()) {
            "Leilão no estado $status não pode ser rejeitado"
        }
        status = AuctionStatus.REJECTED
        rejectedByUserId = adminUserId
        rejectedAt = Instant.now()
        rejectionReason = reason
    }

    fun start() {
        check(status.canStart()) {
            "Leilão no estado $status não pode ser iniciado"
        }
        val now = Instant.now()
        status = AuctionStatus.ACTIVE
        startedAt = now
        endsAt = now.plusSeconds(durationSeconds.toLong())
    }

    fun cancel(cancelledByUserId: UUID, reason: String? = null) {
        check(status.canCancel()) {
            "Leilão no estado $status não pode ser cancelado"
        }
        status = AuctionStatus.CANCELLED
        this.cancelledByUserId = cancelledByUserId
        this.cancelledAt = Instant.now()
        this.cancellationReason = reason
    }

    fun receiveNewBid(bidId: UUID, bidAmount: Int) {
        check(status.isActive()) { "Leilão não está ativo" }
        currentPriceAmount = bidAmount
        leadingBidId = bidId

        // Prorrogação: se o lance cair no último minuto, soma +2 minutos
        val now = Instant.now()
        val oneMinuteFromNow = now.plusSeconds(60)
        if (endsAt != null && endsAt!!.isBefore(oneMinuteFromNow)) {
            endsAt = now.plusSeconds(120)
        }
    }

    fun finishWithWinner(winnerUserId: UUID) {
        check(status == AuctionStatus.ACTIVE) { "Leilão não está ativo" }
        status = AuctionStatus.FINISHED_WITH_WINNER
        this.winnerUserId = winnerUserId
        this.finishedAt = Instant.now()
    }

    fun finishNoBids() {
        check(status == AuctionStatus.ACTIVE) { "Leilão não está ativo" }
        status = AuctionStatus.FINISHED_NO_BIDS
        this.finishedAt = Instant.now()
    }

    /**
     * Atualiza o preço inicial (somente antes de receber lances — DRAFT/REJECTED).
     * Reseta também o currentPriceAmount pois ainda não há lances.
     */
    fun updateInitialPrice(newPrice: Int) {
        initialPriceAmount = newPrice
        currentPriceAmount = newPrice
    }

    fun isOwnedBy(userId: UUID) = seller.id == userId

    fun nextMinimumBid(): Int = currentPriceAmount + minIncrementAmount

    fun hasExpired(): Boolean {
        val ends = endsAt ?: return false
        return Instant.now().isAfter(ends)
    }
}
