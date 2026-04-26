package com.leilao.backend.rifas.domain

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
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "rifas")
class Rifa(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "ticket_price_amount", nullable = false)
    var ticketPriceAmount: Int,

    @Column(name = "total_tickets", nullable = false)
    var totalTickets: Int,

    @Column(name = "draw_date", nullable = false)
    var drawDate: Instant,

) : AuditableEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: RifaStatus = RifaStatus.DRAFT
        protected set

    @Column(name = "winner_ticket_number")
    var winnerTicketNumber: Int? = null
        protected set

    @Column(name = "winner_user_id")
    var winnerUserId: UUID? = null
        protected set

    @Column(name = "approved_by_user_id")
    var approvedByUserId: UUID? = null
        protected set

    @Column(name = "approved_at")
    var approvedAt: Instant? = null
        protected set

    @Column(name = "rejected_by_user_id")
    var rejectedByUserId: UUID? = null
        protected set

    @Column(name = "rejected_at")
    var rejectedAt: Instant? = null
        protected set

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    var rejectionReason: String? = null
        protected set

    @Column(name = "started_at")
    var startedAt: Instant? = null
        protected set

    @Column(name = "finished_at")
    var finishedAt: Instant? = null
        protected set

    fun submitForApproval() {
        check(status.canSubmitForApproval()) { "Rifa no estado $status não pode ser enviada para aprovação" }
        status = RifaStatus.PENDING_APPROVAL
    }

    fun approve(adminUserId: UUID) {
        check(status.canApproveOrReject()) { "Rifa no estado $status não pode ser aprovada" }
        status = RifaStatus.READY_TO_START
        approvedByUserId = adminUserId
        approvedAt = Instant.now()
        rejectedByUserId = null
        rejectedAt = null
        rejectionReason = null
    }

    fun reject(adminUserId: UUID, reason: String) {
        check(status.canApproveOrReject()) { "Rifa no estado $status não pode ser rejeitada" }
        status = RifaStatus.REJECTED
        rejectedByUserId = adminUserId
        rejectedAt = Instant.now()
        rejectionReason = reason
    }

    fun start() {
        check(status.canStart()) { "Rifa no estado $status não pode ser iniciada" }
        status = RifaStatus.ACTIVE
        startedAt = Instant.now()
    }

    fun cancel() {
        check(status.canCancel()) { "Rifa no estado $status não pode ser cancelada" }
        status = RifaStatus.CANCELLED
    }

    fun declareWinner(sellerId: UUID, ticketNumber: Int, winnerId: UUID) {
        check(status.isActive()) { "Rifa não está ativa" }
        check(seller.id == sellerId) { "Somente o vendedor pode declarar o vencedor" }
        check(ticketNumber in 1..totalTickets) { "Número de bilhete inválido: deve ser entre 1 e $totalTickets" }
        status = RifaStatus.FINISHED
        winnerTicketNumber = ticketNumber
        winnerUserId = winnerId
        finishedAt = Instant.now()
    }

    fun isOwnedBy(userId: UUID) = seller.id == userId
}
