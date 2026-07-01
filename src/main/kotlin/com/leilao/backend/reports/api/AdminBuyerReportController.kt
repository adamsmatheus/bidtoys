package com.leilao.backend.reports.api

import com.leilao.backend.reports.api.dto.BuyerReportResponse
import com.leilao.backend.reports.api.dto.ResolveReportRequest
import com.leilao.backend.reports.application.ListBuyerReportsUseCase
import com.leilao.backend.reports.application.ResolveBuyerReportUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/buyer-reports")
@Tag(name = "Admin - Buyer Reports", description = "Gestão de reportes de compradores")
class AdminBuyerReportController(
    private val listBuyerReportsUseCase: ListBuyerReportsUseCase,
    private val resolveBuyerReportUseCase: ResolveBuyerReportUseCase
) {

    @GetMapping
    @Operation(summary = "Lista todos os reportes de compradores")
    fun list(): List<BuyerReportResponse> = listBuyerReportsUseCase.execute()

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve um reporte: desativa o usuário ou mantém conta ativa")
    fun resolve(
        @PathVariable id: UUID,
        @RequestBody request: ResolveReportRequest
    ) {
        resolveBuyerReportUseCase.execute(id, request.action)
    }
}
