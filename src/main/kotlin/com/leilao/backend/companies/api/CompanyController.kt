package com.leilao.backend.companies.api

import com.leilao.backend.companies.api.dto.ActiveCompanyResponse
import com.leilao.backend.companies.api.dto.CompanyResponse
import com.leilao.backend.companies.api.dto.UpsertCompanyRequest
import com.leilao.backend.companies.application.DeleteMyCompanyUseCase
import com.leilao.backend.companies.application.GetMyCompanyUseCase
import com.leilao.backend.companies.application.ListActiveCompaniesUseCase
import com.leilao.backend.companies.application.ListAllCompaniesUseCase
import com.leilao.backend.companies.application.UploadCompanyLogoUseCase
import com.leilao.backend.companies.application.UpsertCompanyUseCase
import com.leilao.backend.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Gerenciamento de empresas")
class CompanyController(
    private val getMyCompanyUseCase: GetMyCompanyUseCase,
    private val upsertCompanyUseCase: UpsertCompanyUseCase,
    private val uploadCompanyLogoUseCase: UploadCompanyLogoUseCase,
    private val deleteMyCompanyUseCase: DeleteMyCompanyUseCase,
    private val listActiveCompaniesUseCase: ListActiveCompaniesUseCase,
    private val listAllCompaniesUseCase: ListAllCompaniesUseCase
) {

    @GetMapping("/me")
    @Operation(summary = "Retorna a empresa do usuário autenticado")
    fun getMyCompany(@AuthenticationPrincipal principal: UserPrincipal): CompanyResponse? {
        return getMyCompanyUseCase.execute(principal.id)?.let { CompanyResponse.from(it) }
    }

    @PutMapping("/me")
    @Operation(summary = "Cria ou atualiza a empresa do usuário autenticado")
    fun upsertMyCompany(
        @Valid @RequestBody request: UpsertCompanyRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): CompanyResponse {
        return CompanyResponse.from(upsertCompanyUseCase.execute(request, principal.id))
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a empresa do usuário autenticado")
    fun deleteMyCompany(@AuthenticationPrincipal principal: UserPrincipal) {
        deleteMyCompanyUseCase.execute(principal.id)
    }

    @PostMapping("/me/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Faz upload do logo da empresa do usuário autenticado")
    fun uploadLogo(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Map<String, String> {
        val logoUrl = uploadCompanyLogoUseCase.execute(principal.id, file)
        return mapOf("logoUrl" to logoUrl)
    }

    @GetMapping("/active")
    @Operation(summary = "Lista empresas com leilões ativos")
    fun listActive(): List<ActiveCompanyResponse> {
        return listActiveCompaniesUseCase.execute().map { ActiveCompanyResponse.from(it) }
    }

    @GetMapping
    @Operation(summary = "Lista todas as empresas cadastradas")
    fun listAll(): List<ActiveCompanyResponse> {
        return listAllCompaniesUseCase.execute().map { ActiveCompanyResponse.from(it) }
    }
}
