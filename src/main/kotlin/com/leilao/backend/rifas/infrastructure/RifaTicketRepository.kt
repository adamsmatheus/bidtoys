package com.leilao.backend.rifas.infrastructure

import com.leilao.backend.rifas.domain.RifaTicket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface RifaTicketRepository : JpaRepository<RifaTicket, UUID> {

    fun findByRifaIdOrderByTicketNumber(rifaId: UUID): List<RifaTicket>

    fun findByRifaIdAndTicketNumber(rifaId: UUID, ticketNumber: Int): Optional<RifaTicket>

    fun findByRifaIdAndBuyerId(rifaId: UUID, buyerId: UUID): List<RifaTicket>

    fun countByRifaId(rifaId: UUID): Long

    fun existsByRifaIdAndTicketNumber(rifaId: UUID, ticketNumber: Int): Boolean

    fun findByBuyerId(buyerId: UUID, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<RifaTicket>
}
