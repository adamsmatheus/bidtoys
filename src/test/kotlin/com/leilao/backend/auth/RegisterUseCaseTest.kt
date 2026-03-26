package com.leilao.backend.auth

import com.leilao.backend.auth.api.dto.AddressRequest
import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.auth.application.RegisterUseCase
import com.leilao.backend.auth.application.WhatsAppVerificationStore
import com.leilao.backend.shared.exception.BusinessException
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
    private val verificationStore = mockk<WhatsAppVerificationStore>()

    private val useCase = RegisterUseCase(userRepository, passwordEncoder, verificationStore)

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
        whatsappNumber = "11999999999",
        verificationCode = "123456",
        address = validAddress
    )

    @BeforeEach
    fun setup() {
        every { verificationStore.verify(any(), any()) } returns true
        every { userRepository.existsByEmail(any()) } returns false
        every { passwordEncoder.encode(any()) } returns "hashed_password"
        every { userRepository.save(any()) } answers { firstArg() }
        justRun { verificationStore.remove(any()) }
    }

    @Test
    fun `deve registrar usuário com dados válidos`() {
        val result = useCase.execute(validRequest)

        assertEquals("joao@example.com", result.email)
        assertEquals("João Silva", result.name)
        assertEquals("11999999999", result.phoneNumber)
        verify { userRepository.save(any()) }
        verify { verificationStore.remove("11999999999") }
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
    fun `deve lançar BusinessException quando código de verificação for inválido`() {
        every { verificationStore.verify(any(), any()) } returns false

        assertThrows<BusinessException> {
            useCase.execute(validRequest)
        }
    }

    @Test
    fun `deve lançar ConflictException quando email já estiver cadastrado`() {
        every { userRepository.existsByEmail(validRequest.email) } returns true

        assertThrows<ConflictException> {
            useCase.execute(validRequest)
        }
    }

    @Test
    fun `deve remover código de verificação após cadastro bem-sucedido`() {
        useCase.execute(validRequest)

        verify { verificationStore.remove(validRequest.whatsappNumber) }
    }
}
