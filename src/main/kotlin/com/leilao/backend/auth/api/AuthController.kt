package com.leilao.backend.auth.api

import com.leilao.backend.auth.api.dto.ForgotPasswordRequest
import com.leilao.backend.auth.api.dto.LoginRequest
import com.leilao.backend.auth.api.dto.LoginResponse
import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.auth.api.dto.ResetPasswordRequest
import com.leilao.backend.auth.api.dto.TelegramCheckResponse
import com.leilao.backend.auth.api.dto.TelegramVerificationRequest
import com.leilao.backend.auth.api.dto.VerifyEmailRequest
import com.leilao.backend.auth.api.dto.WhatsAppVerificationRequest
import com.leilao.backend.auth.api.dto.WhatsAppVerifyCodeRequest
import com.leilao.backend.auth.application.WhatsAppVerifyCodeResponse
import com.leilao.backend.auth.application.ForgotPasswordUseCase
import com.leilao.backend.auth.application.LoginUseCase
import com.leilao.backend.auth.application.RegisterUseCase
import com.leilao.backend.auth.application.RequestTelegramVerificationUseCase
import com.leilao.backend.auth.application.RequestWhatsAppVerificationUseCase
import com.leilao.backend.auth.application.ResetPasswordUseCase
import com.leilao.backend.auth.application.TelegramVerificationResponse
import com.leilao.backend.auth.application.TelegramVerificationStore
import com.leilao.backend.auth.application.VerifyEmailUseCase
import com.leilao.backend.auth.application.VerifyWhatsAppCodeUseCase
import com.leilao.backend.users.api.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Autenticação e registro de usuários")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val requestTelegramVerificationUseCase: RequestTelegramVerificationUseCase,
    private val telegramVerificationStore: TelegramVerificationStore,
    private val requestWhatsAppVerificationUseCase: RequestWhatsAppVerificationUseCase,
    private val verifyWhatsAppCodeUseCase: VerifyWhatsAppCodeUseCase
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um novo usuário e envia código de verificação por e-mail")
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse {
        val user = registerUseCase.execute(request)
        return UserResponse.from(user)
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirma o e-mail do usuário com o código recebido")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest) {
        verifyEmailUseCase.execute(request.email, request.code)
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna o token JWT")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        return loginUseCase.execute(request)
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Envia código de reset de senha por e-mail")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest) {
        forgotPasswordUseCase.execute(request.email)
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha usando o código recebido")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest) {
        resetPasswordUseCase.execute(request.email, request.code, request.newPassword)
    }

    @PostMapping("/telegram/request-verification")
    @Operation(summary = "Solicita verificação de celular via Telegram e retorna o deep link")
    fun requestTelegramVerification(
        @Valid @RequestBody request: TelegramVerificationRequest
    ): TelegramVerificationResponse {
        return requestTelegramVerificationUseCase.execute(request.phoneNumber)
    }

    @GetMapping("/telegram/check/{token}")
    @Operation(summary = "Verifica se o token do Telegram foi confirmado pelo usuário")
    fun checkTelegramVerification(@PathVariable token: String): TelegramCheckResponse {
        return TelegramCheckResponse(verified = telegramVerificationStore.isVerified(token))
    }

    @PostMapping("/whatsapp/request-verification")
    @Operation(summary = "Envia código de verificação via WhatsApp para o número informado")
    fun requestWhatsAppVerification(
        @Valid @RequestBody request: WhatsAppVerificationRequest
    ) = requestWhatsAppVerificationUseCase.execute(request.phoneNumber)

    @PostMapping("/whatsapp/verify-code")
    @Operation(summary = "Confirma o código de verificação recebido via WhatsApp")
    fun verifyWhatsAppCode(
        @Valid @RequestBody request: WhatsAppVerifyCodeRequest
    ): WhatsAppVerifyCodeResponse = verifyWhatsAppCodeUseCase.execute(request.token, request.code)
}
