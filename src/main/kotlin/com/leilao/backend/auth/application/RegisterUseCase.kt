package com.leilao.backend.auth.application

import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.shared.exception.ConflictException
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun execute(request: RegisterRequest): User {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("E-mail já cadastrado", "EMAIL_ALREADY_EXISTS")
        }

        val user = User(
            name = request.name,
            email = request.email.lowercase().trim(),
            passwordHash = passwordEncoder.encode(request.password),
            phoneNumber = request.phoneNumber
        )

        return userRepository.save(user)
    }
}
