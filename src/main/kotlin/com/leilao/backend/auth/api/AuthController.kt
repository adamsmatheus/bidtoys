package com.leilao.backend.auth.api

import com.leilao.backend.auth.api.dto.ForgotPasswordRequest
import com.leilao.backend.auth.api.dto.LoginRequest
import com.leilao.backend.auth.api.dto.LoginResponse
import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.auth.api.dto.RequestTelegramVerificationRequest
import com.leilao.backend.auth.api.dto.ResetPasswordRequest
import com.leilao.backend.auth.application.ForgotPasswordUseCase
import com.leilao.backend.auth.application.LoginUseCase
import com.leilao.backend.auth.application.RegisterUseCase
import com.leilao.backend.auth.application.RequestTelegramVerificationUseCase
import com.leilao.backend.auth.application.ResetPasswordUseCase
import com.leilao.backend.auth.application.TelegramVerificationResponse
import com.leilao.backend.auth.application.TelegramVerificationStore
import com.leilao.backend.users.api.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Autenticação e registro de usuários")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val requestTelegramVerificationUseCase: RequestTelegramVerificationUseCase,
    private val telegramVerificationStore: TelegramVerificationStore,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um novo usuário (requer token de verificação Telegram)")
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse {
        val user = registerUseCase.execute(request)
        return UserResponse.from(user)
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna o token JWT")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        return loginUseCase.execute(request)
    }

    @PostMapping("/telegram/request-verification")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita verificação de número via Telegram; retorna token e deep link")
    fun requestTelegramVerification(
        @Valid @RequestBody request: RequestTelegramVerificationRequest
    ): TelegramVerificationResponse {
        return requestTelegramVerificationUseCase.execute(request.phoneNumber)
    }

    @GetMapping("/telegram/check-verification")
    @Operation(summary = "Verifica se o token Telegram foi confirmado pelo bot")
    fun checkTelegramVerification(@RequestParam token: String): Map<String, Boolean> {
        return mapOf("verified" to telegramVerificationStore.isVerified(token))
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Envia código de reset de senha via Telegram ou e-mail")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest) {
        forgotPasswordUseCase.execute(request.email)
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha usando o código recebido")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest) {
        resetPasswordUseCase.execute(request.email, request.code, request.newPassword)
    }
}
