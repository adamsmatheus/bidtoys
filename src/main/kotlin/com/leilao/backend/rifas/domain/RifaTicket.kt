package com.leilao.backend.rifas.domain

import com.leilao.backend.shared.domain.BaseEntity
import com.leilao.backend.users.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "rifa_tickets")
class RifaTicket(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rifa_id", nullable = false)
    val rifa: Rifa,

    @Column(name = "ticket_number", nullable = false)
    val ticketNumber: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    val buyer: User,

) : BaseEntity()
