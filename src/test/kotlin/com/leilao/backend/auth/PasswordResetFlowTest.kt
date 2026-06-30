package com.leilao.backend.auth

import com.leilao.backend.auth.application.ForgotPasswordUseCase
import com.leilao.backend.auth.application.PasswordResetStore
import com.leilao.backend.auth.application.ResetPasswordUseCase
import com.leilao.backend.shared.email.EmailService
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import com.leilao.backend.users.infrastructure.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

/**
 * Teste integrado do fluxo de reset de senha.
 *
 * Usa o PasswordResetStore REAL para validar a integração entre
 * ForgotPasswordUseCase e ResetPasswordUseCase.
 * Dependências externas (DB, Email) são mockadas.
 */
class PasswordResetFlowTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val emailService = mockk<EmailService>()

    // Store real — peça central do teste integrado
    private val passwordResetStore = PasswordResetStore()

    private val forgotPasswordUseCase = ForgotPasswordUseCase(
        userRepository, passwordResetStore, emailService
    )
    private val resetPasswordUseCase = ResetPasswordUseCase(
        userRepository, passwordResetStore, passwordEncoder
    )

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        user = User(
            name = "Maria Silva",
            email = "maria@example.com",
            passwordHash = "old_hash",
            phoneNumber = "11999990000",
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )
    }

    // -------------------------------------------------------------------------
    // Fluxo via e-mail (happy path)
    // -------------------------------------------------------------------------

    @Test
    fun `deve enviar codigo por email e redefinir senha com sucesso`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode("novaSenha123") } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("maria@example.com")

        val sentCode = codeSlot.captured
        assertEquals(6, sentCode.length)
        verify { emailService.sendPasswordResetCode("maria@example.com", sentCode) }

        resetPasswordUseCase.execute("maria@example.com", sentCode, "novaSenha123")

        assertEquals("new_hash", user.passwordHash)
        verify { userRepository.save(user) }
    }

    @Test
    fun `codigo deve ser invalidado apos uso`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode(any()) } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("maria@example.com")
        val code = codeSlot.captured

        resetPasswordUseCase.execute("maria@example.com", code, "novaSenha123")

        assertThrows<BusinessException> {
            resetPasswordUseCase.execute("maria@example.com", code, "outraSenha")
        }
    }

    // -------------------------------------------------------------------------
    // ForgotPasswordUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `deve retornar silenciosamente quando email nao existir`() {
        every { userRepository.findByEmail(any()) } returns Optional.empty()

        forgotPasswordUseCase.execute("naoexiste@example.com")

        verify(exactly = 0) { emailService.sendPasswordResetCode(any(), any()) }
    }

    @Test
    fun `deve gerar codigo de 6 digitos numericos`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), capture(codeSlot)) }

        forgotPasswordUseCase.execute("maria@example.com")

        val code = codeSlot.captured
        assertEquals(6, code.length)
        assert(code.all { it.isDigit() }) { "Código deve conter apenas dígitos: $code" }
    }

    @Test
    fun `deve gerar codigos diferentes em chamadas distintas`() {
        val codes = mutableListOf<String>()
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), capture(codes)) }

        repeat(5) { forgotPasswordUseCase.execute("maria@example.com") }

        assert(codes.toSet().size > 1) { "Esperado códigos diferentes, obtidos: $codes" }
    }

    // -------------------------------------------------------------------------
    // ResetPasswordUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `reset - deve lancar BusinessException quando codigo for invalido`() {
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), any()) }

        forgotPasswordUseCase.execute("maria@example.com")

        assertThrows<BusinessException> {
            resetPasswordUseCase.execute("maria@example.com", "000000", "novaSenha123")
        }
    }

    @Test
    fun `reset - nao deve alterar senha quando codigo for invalido`() {
        val originalHash = user.passwordHash
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), any()) }

        forgotPasswordUseCase.execute("maria@example.com")

        assertThrows<BusinessException> {
            resetPasswordUseCase.execute("maria@example.com", "ERRADO", "novaSenha123")
        }

        assertEquals(originalHash, user.passwordHash)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `reset - deve salvar nova senha com hash BCrypt`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode("novaSenha123") } returns "bcrypt_novo_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("maria@example.com")
        resetPasswordUseCase.execute("maria@example.com", codeSlot.captured, "novaSenha123")

        assertEquals("bcrypt_novo_hash", user.passwordHash)
        assertNotEquals("old_hash", user.passwordHash)
    }

    // -------------------------------------------------------------------------
    // PasswordResetStore (TTL)
    // -------------------------------------------------------------------------

    @Test
    fun `store - deve rejeitar codigo expirado`() {
        passwordResetStore.save("maria@example.com", "123456", ttlSeconds = 0)

        Thread.sleep(10)

        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)

        assertThrows<BusinessException> {
            resetPasswordUseCase.execute("maria@example.com", "123456", "novaSenha")
        }
    }

    @Test
    fun `store - deve aceitar codigo dentro do TTL`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { emailService.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode(any()) } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("maria@example.com")

        Thread.sleep(100)

        resetPasswordUseCase.execute("maria@example.com", codeSlot.captured, "novaSenha123")

        assertEquals("new_hash", user.passwordHash)
    }
}
