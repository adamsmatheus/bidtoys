package com.leilao.backend.shared.security

import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val email: String,
    val role: String
)
