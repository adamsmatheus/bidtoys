package com.leilao.backend.auth.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(

    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    val name: String,

    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String,

    @field:NotBlank(message = "Número de WhatsApp é obrigatório")
    val whatsappNumber: String,

    @field:NotBlank(message = "Código de verificação é obrigatório")
    val verificationCode: String
)

data class SendWhatsAppCodeRequest(
    @field:NotBlank(message = "Número de WhatsApp é obrigatório")
    val phoneNumber: String
)

data class LoginRequest(
    @field:NotBlank(message = "E-mail é obrigatório")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    val password: String
)

data class LoginResponse(
    val token: String,
    val tokenType: String = "Bearer"
)
