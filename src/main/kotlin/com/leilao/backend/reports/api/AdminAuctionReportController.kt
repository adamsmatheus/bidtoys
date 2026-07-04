package com.leilao.backend.reports.api

import com.leilao.backend.reports.api.dto.AuctionReportResponse
import com.leilao.backend.reports.application.ListAuctionReportsUseCase
import com.leilao.backend.reports.application.ResolveAuctionReportUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/auction-reports")
@Tag(name = "Admin - Auction Reports", description = "Gestão de reportes de disputas")
class AdminAuctionReportController(
    private val listAuctionReportsUseCase: ListAuctionReportsUseCase,
    private val resolveAuctionReportUseCase: ResolveAuctionReportUseCase
) {

    @GetMapping
    @Operation(summary = "Lista todos os reportes de disputas de leilão")
    fun list(): List<AuctionReportResponse> =
        listAuctionReportsUseCase.execute().map { AuctionReportResponse.from(it) }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Marca o reporte como resolvido")
    fun resolve(@PathVariable id: UUID) {
        resolveAuctionReportUseCase.execute(id)
    }
}
