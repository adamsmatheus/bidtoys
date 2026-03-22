package com.leilao.backend.users.api.dto

import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 6) val password: String,
    val phoneNumber: String? = null,
    val whatsappEnabled: Boolean = false
)

data class UpdateUserRequest(
    @field:NotBlank val name: String? = null,
    val phoneNumber: String? = null,
    val whatsappEnabled: Boolean? = null,
    val role: UserRole? = null,
    val status: UserStatus? = null
)

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val whatsappEnabled: Boolean,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: Instant
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            whatsappEnabled = user.whatsappEnabled,
            role = user.role,
            status = user.status,
            createdAt = user.createdAt
        )
    }
}
