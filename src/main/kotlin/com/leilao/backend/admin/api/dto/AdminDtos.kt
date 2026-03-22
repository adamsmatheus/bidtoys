package com.leilao.backend.admin.api.dto

import jakarta.validation.constraints.NotBlank

data class RejectAuctionRequest(
    @field:NotBlank(message = "Motivo da rejeição é obrigatório")
    val reason: String
)
