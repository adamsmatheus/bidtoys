package com.leilao.backend.companies.api.dto

import com.leilao.backend.companies.domain.Company
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpsertCompanyRequest(

    @field:NotBlank(message = "Nome da empresa é obrigatório")
    @field:Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    val name: String,

    val description: String? = null,

    val logoUrl: String? = null
)

data class CompanyResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val logoUrl: String?
) {
    companion object {
        fun from(company: Company) = CompanyResponse(
            id = company.id,
            name = company.name,
            description = company.description,
            logoUrl = company.logoUrl
        )
    }
}

data class ActiveCompanyResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val sellerId: UUID
) {
    companion object {
        fun from(company: Company) = ActiveCompanyResponse(
            id = company.id,
            name = company.name,
            description = company.description,
            logoUrl = company.logoUrl,
            sellerId = company.user.id
        )
    }
}
