package com.leilao.backend.rifas.infrastructure

import com.leilao.backend.rifas.domain.RifaImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RifaImageRepository : JpaRepository<RifaImage, UUID> {
    fun findByRifaIdOrderByPosition(rifaId: UUID): List<RifaImage>
}
