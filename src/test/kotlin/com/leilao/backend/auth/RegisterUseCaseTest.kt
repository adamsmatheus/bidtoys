package com.leilao.backend.auth

import com.leilao.backend.auth.api.dto.AddressRequest
import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.auth.application.EmailVerificationStore
import com.leilao.backend.auth.application.RegisterUseCase
import com.leilao.backend.shared.email.EmailService
import com.leilao.backend.shared.exception.ConflictException
import com.leilao.backend.users.infrastructure.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder

class RegisterUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val emailVerificationStore = mockk<EmailVerificationStore>()
    private val emailService = mockk<EmailService>()

    private val useCase = RegisterUseCase(userRepository, passwordEncoder, emailVerificationStore, emailService)

    private val validAddress = AddressRequest(
        cep = "01310-100",
        street = "Av. Paulista",
        city = "São Paulo",
        state = "SP",
        number = "1000"
    )

    private val validRequest = RegisterRequest(
        name = "João Silva",
        email = "joao@example.com",
        password = "senha123",
        phoneNumber = "+5511999999999",
        address = validAddress
    )

    @BeforeEach
    fun setup() {
        every { userRepository.existsByEmail(any()) } returns false
        every { passwordEncoder.encode(any()) } returns "hashed_password"
        every { userRepository.save(any()) } answers { firstArg() }
        every { emailVerificationStore.generate(any()) } returns "123456"
        justRun { emailService.sendEmailVerification(any(), any()) }
    }

    @Test
    fun `deve registrar usuário com dados válidos`() {
        val result = useCase.execute(validRequest)

        assertEquals("joao@example.com", result.email)
        assertEquals("João Silva", result.name)
        assertEquals("+5511999999999", result.phoneNumber)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `deve normalizar email para lowercase`() {
        val request = validRequest.copy(email = "JOAO@EXAMPLE.COM")

        val result = useCase.execute(request)

        assertEquals("joao@example.com", result.email)
    }

    @Test
    fun `deve salvar usuário com senha encriptada`() {
        every { passwordEncoder.encode("senha123") } returns "bcrypt_hash"

        val result = useCase.execute(validRequest)

        assertEquals("bcrypt_hash", result.passwordHash)
    }

    @Test
    fun `deve lançar ConflictException quando email já estiver cadastrado`() {
        every { userRepository.existsByEmail(validRequest.email) } returns true

        assertThrows<ConflictException> {
            useCase.execute(validRequest)
        }
    }

    @Test
    fun `deve enviar email de verificação após cadastro`() {
        useCase.execute(validRequest)

        verify { emailVerificationStore.generate("joao@example.com") }
        verify { emailService.sendEmailVerification("joao@example.com", "123456") }
    }
}
