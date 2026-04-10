package com.leilao.backend.auth.application

import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppGateway
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.users.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class ForgotPasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordResetStore: PasswordResetStore,
    private val whatsAppGateway: WhatsAppGateway
) {
    private val log = LoggerFactory.getLogger(ForgotPasswordUseCase::class.java)

    fun execute(email: String) {
        val user = userRepository.findByEmail(email.lowercase().trim()).orElse(null)

        // Retorno silencioso: não revela se o e-mail existe ou não
        if (user == null) {
            log.warn("[ForgotPassword] E-mail não encontrado: {}", email)
            return
        }

        val code = String.format("%06d", Random.nextInt(0, 1_000_000))
        passwordResetStore.save(user.email, code)
        whatsAppGateway.sendPasswordResetCode(user.phoneNumber, code)

        log.info("[ForgotPassword] Código enviado para usuário {}", user.id)
    }
}

@Service
class ResetPasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordResetStore: PasswordResetStore,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun execute(email: String, code: String, newPassword: String) {
        if (!passwordResetStore.verify(email, code)) {
            throw BusinessException(
                "Código inválido ou expirado",
                "INVALID_RESET_CODE",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

        val user = userRepository.findByEmail(email.lowercase().trim())
            .orElseThrow {
                BusinessException("Usuário não encontrado", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
            }

        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        passwordResetStore.remove(email)
    }
}
