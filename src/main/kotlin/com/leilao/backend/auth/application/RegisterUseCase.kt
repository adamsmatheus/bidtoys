package com.leilao.backend.auth.application

import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.ConflictException
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserAddress
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val verificationStore: TelegramVerificationStore
) {

    @Transactional
    fun execute(request: RegisterRequest): User {
        val (phoneNumber, chatId) = verificationStore.getIfVerified(request.verificationToken)
            ?: throw BusinessException(
                "Verificação do Telegram não encontrada ou expirada",
                "INVALID_VERIFICATION_TOKEN",
                HttpStatus.UNPROCESSABLE_ENTITY
            )

        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("E-mail já cadastrado", "EMAIL_ALREADY_EXISTS")
        }

        val user = User(
            name = request.name,
            email = request.email.lowercase().trim(),
            passwordHash = passwordEncoder.encode(request.password),
            phoneNumber = phoneNumber,
            telegramChatId = chatId
        )

        val address = UserAddress(
            user = user,
            cep = request.address.cep,
            street = request.address.street,
            city = request.address.city,
            state = request.address.state,
            number = request.address.number,
            complement = request.address.complement
        )
        user.address = address

        val saved = userRepository.save(user)
        verificationStore.remove(request.verificationToken)
        return saved
    }
}
