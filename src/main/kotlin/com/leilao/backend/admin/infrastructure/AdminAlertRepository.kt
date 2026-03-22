package com.leilao.backend.admin.infrastructure

import com.leilao.backend.admin.domain.AdminAlert
import com.leilao.backend.admin.domain.AdminAlertStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdminAlertRepository : JpaRepository<AdminAlert, UUID> {
    fun findByStatus(status: AdminAlertStatus, pageable: Pageable): Page<AdminAlert>
}
