package com.leilao.backend.auth.application

import com.leilao.backend.auth.api.dto.LoginRequest
import com.leilao.backend.auth.api.dto.LoginResponse
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.security.JwtTokenProvider
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional(readOnly = true)
    fun execute(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email.lowercase().trim())
            .orElseThrow {
                BusinessException("Credenciais inválidas", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED)
            }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BusinessException("Credenciais inválidas", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED)
        }

        if (!user.isActive()) {
            throw BusinessException("Usuário bloqueado", "USER_BLOCKED", HttpStatus.FORBIDDEN)
        }

        val token = jwtTokenProvider.generateToken(user.id, user.email, user.role.name)
        return LoginResponse(token = token)
    }
}
