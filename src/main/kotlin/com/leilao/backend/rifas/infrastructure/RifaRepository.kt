package com.leilao.backend.rifas.infrastructure

import com.leilao.backend.rifas.domain.Rifa
import com.leilao.backend.rifas.domain.RifaStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RifaRepository : JpaRepository<Rifa, UUID> {

    fun findByStatus(status: RifaStatus, pageable: Pageable): Page<Rifa>

    fun findByStatusIn(statuses: Collection<RifaStatus>, pageable: Pageable): Page<Rifa>

    fun findBySellerId(sellerId: UUID, pageable: Pageable): Page<Rifa>

    fun findBySellerIdAndStatus(sellerId: UUID, status: RifaStatus, pageable: Pageable): Page<Rifa>

    fun findBySellerIdAndStatusIn(sellerId: UUID, statuses: Collection<RifaStatus>, pageable: Pageable): Page<Rifa>

    fun findBySellerIdAndStatusNotIn(sellerId: UUID, statuses: Collection<RifaStatus>, pageable: Pageable): Page<Rifa>
}
