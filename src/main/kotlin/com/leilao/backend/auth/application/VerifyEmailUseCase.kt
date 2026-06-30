package com.leilao.backend.auth.application

import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VerifyEmailUseCase(
    private val userRepository: UserRepository,
    private val emailVerificationStore: EmailVerificationStore
) {

    @Transactional
    fun execute(email: String, code: String) {
        if (!emailVerificationStore.verify(email, code)) {
            throw BusinessException(
                "Código inválido ou expirado",
                "INVALID_VERIFICATION_CODE",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

        val user = userRepository.findByEmail(email.lowercase().trim())
            .orElseThrow {
                BusinessException("Usuário não encontrado", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
            }

        user.emailVerified = true
        userRepository.save(user)
        emailVerificationStore.remove(email)
    }
}
