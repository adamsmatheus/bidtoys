package com.leilao.backend.users.api.dto

import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserAddress
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import jakarta.validation.constraints.NotBlank
// UserRole mantido para UserResponse.role
import java.time.Instant
import java.util.UUID

data class UpdateUserRequest(
    @field:NotBlank val name: String? = null,
    val phoneNumber: String? = null,
    val whatsappEnabled: Boolean? = null,
    val status: UserStatus? = null
)

data class AddressResponse(
    val cep: String,
    val street: String,
    val city: String,
    val state: String,
    val number: String,
    val complement: String?
) {
    companion object {
        fun from(address: UserAddress) = AddressResponse(
            cep = address.cep,
            street = address.street,
            city = address.city,
            state = address.state,
            number = address.number,
            complement = address.complement
        )
    }
}

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val whatsappEnabled: Boolean,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: Instant,
    val address: AddressResponse?
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
            createdAt = user.createdAt,
            address = user.address?.let { AddressResponse.from(it) }
        )
    }
}
