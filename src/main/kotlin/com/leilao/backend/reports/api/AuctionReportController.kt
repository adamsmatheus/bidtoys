package com.leilao.backend.reports.api

import com.leilao.backend.reports.api.dto.SubmitAuctionReportRequest
import com.leilao.backend.reports.application.SubmitAuctionReportUseCase
import com.leilao.backend.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/auction-reports")
@Tag(name = "Auction Reports", description = "Reporte de disputas de leilão")
class AuctionReportController(
    private val submitAuctionReportUseCase: SubmitAuctionReportUseCase
) {

    @PostMapping("/{auctionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reporta um leilão em disputa para análise do admin")
    fun submit(
        @PathVariable auctionId: UUID,
        @Valid @RequestBody request: SubmitAuctionReportRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Map<String, UUID> {
        val reportId = submitAuctionReportUseCase.execute(auctionId, principal.id, request.reason)
        return mapOf("reportId" to reportId)
    }
}
