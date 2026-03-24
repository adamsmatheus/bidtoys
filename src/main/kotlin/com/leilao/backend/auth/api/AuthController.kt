package com.leilao.backend.auth.api

import com.leilao.backend.auth.api.dto.LoginRequest
import com.leilao.backend.auth.api.dto.LoginResponse
import com.leilao.backend.auth.api.dto.RegisterRequest
import com.leilao.backend.auth.api.dto.SendWhatsAppCodeRequest
import com.leilao.backend.auth.application.LoginUseCase
import com.leilao.backend.auth.application.RegisterUseCase
import com.leilao.backend.auth.application.SendWhatsAppCodeUseCase
import com.leilao.backend.users.api.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
    private val sendWhatsAppCodeUseCase: SendWhatsAppCodeUseCase
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um novo usuário (requer código WhatsApp verificado)")
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse {
        val user = registerUseCase.execute(request)
        return UserResponse.from(user)
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna o token JWT")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        return loginUseCase.execute(request)
    }

    @PostMapping("/whatsapp/send-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Envia código de verificação de 6 dígitos via WhatsApp")
    fun sendWhatsAppCode(@Valid @RequestBody request: SendWhatsAppCodeRequest) {
        sendWhatsAppCodeUseCase.execute(request.phoneNumber)
    }
}
