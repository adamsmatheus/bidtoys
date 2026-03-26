package com.leilao.backend.users.infrastructure

import com.leilao.backend.users.domain.UserAddress
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserAddressRepository : JpaRepository<UserAddress, UUID> {
    fun findByUserId(userId: UUID): Optional<UserAddress>
}
