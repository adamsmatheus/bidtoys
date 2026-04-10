package com.leilao.backend.auth

import com.leilao.backend.auth.application.ForgotPasswordUseCase
import com.leilao.backend.auth.application.PasswordResetStore
import com.leilao.backend.auth.application.ResetPasswordUseCase
import com.leilao.backend.notifications.infrastructure.whatsapp.WhatsAppGateway
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
 * Dependências externas (DB, WhatsApp) são mockadas.
 */
class PasswordResetFlowTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val whatsAppGateway = mockk<WhatsAppGateway>()

    // Store real — peça central do teste integrado
    private val passwordResetStore = PasswordResetStore()

    private val forgotPasswordUseCase = ForgotPasswordUseCase(
        userRepository, passwordResetStore, whatsAppGateway
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
    // Fluxo completo (happy path)
    // -------------------------------------------------------------------------

    @Test
    fun `fluxo completo - deve enviar codigo e redefinir senha com sucesso`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode("novaSenha123") } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        // Passo 1: solicitar reset
        forgotPasswordUseCase.execute("maria@example.com")

        val sentCode = codeSlot.captured
        assertEquals(6, sentCode.length)
        verify { whatsAppGateway.sendPasswordResetCode("11999990000", sentCode) }

        // Passo 2: redefinir senha com o código recebido
        resetPasswordUseCase.execute("maria@example.com", sentCode, "novaSenha123")

        assertEquals("new_hash", user.passwordHash)
        verify { userRepository.save(user) }
    }

    @Test
    fun `fluxo completo - codigo deve ser invalidado apos uso`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode(any()) } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("maria@example.com")
        val code = codeSlot.captured

        // Primeiro uso: ok
        resetPasswordUseCase.execute("maria@example.com", code, "novaSenha123")

        // Segundo uso com mesmo código deve falhar
        assertThrows<BusinessException> {
            resetPasswordUseCase.execute("maria@example.com", code, "outraSenha")
        }
    }

    // -------------------------------------------------------------------------
    // ForgotPasswordUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `forgot - deve retornar silenciosamente quando email nao existir`() {
        every { userRepository.findByEmail(any()) } returns Optional.empty()

        // Não lança exceção — não revela se o e-mail existe (segurança)
        forgotPasswordUseCase.execute("naoexiste@example.com")

        verify(exactly = 0) { whatsAppGateway.sendPasswordResetCode(any(), any()) }
    }

    @Test
    fun `forgot - deve normalizar email para lowercase antes de armazenar`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail("maria@example.com") } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode(any()) } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("MARIA@EXAMPLE.COM")

        // Reset com email em lowercase deve funcionar
        resetPasswordUseCase.execute("maria@example.com", codeSlot.captured, "novaSenha123")

        assertEquals("new_hash", user.passwordHash)
    }

    @Test
    fun `forgot - deve gerar codigo de 6 digitos numericos`() {
        val codeSlot = slot<String>()
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codeSlot)) }

        forgotPasswordUseCase.execute("maria@example.com")

        val code = codeSlot.captured
        assertEquals(6, code.length)
        assert(code.all { it.isDigit() }) { "Código deve conter apenas dígitos: $code" }
    }

    @Test
    fun `forgot - deve gerar codigos diferentes em chamadas distintas`() {
        val codes = mutableListOf<String>()
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codes)) }

        repeat(5) { forgotPasswordUseCase.execute("maria@example.com") }

        // Ao menos 2 códigos distintos entre 5 chamadas
        assert(codes.toSet().size > 1) { "Esperado códigos diferentes, obtidos: $codes" }
    }

    // -------------------------------------------------------------------------
    // ResetPasswordUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `reset - deve lancar BusinessException quando codigo for invalido`() {
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), any()) }

        forgotPasswordUseCase.execute("maria@example.com")

        assertThrows<BusinessException> {
            resetPasswordUseCase.execute("maria@example.com", "000000", "novaSenha123")
        }
    }

    @Test
    fun `reset - nao deve alterar senha quando codigo for invalido`() {
        val originalHash = user.passwordHash
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        justRun { whatsAppGateway.sendPasswordResetCode(any(), any()) }

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
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codeSlot)) }
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
        // Salva com TTL de 0 segundos (já expirado)
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
        justRun { whatsAppGateway.sendPasswordResetCode(any(), capture(codeSlot)) }
        every { passwordEncoder.encode(any()) } returns "new_hash"
        every { userRepository.save(any()) } answers { firstArg() }

        forgotPasswordUseCase.execute("maria@example.com")

        Thread.sleep(100)

        // Código ainda válido após 100ms (TTL padrão é 15 min)
        resetPasswordUseCase.execute("maria@example.com", codeSlot.captured, "novaSenha123")

        assertEquals("new_hash", user.passwordHash)
    }
}
