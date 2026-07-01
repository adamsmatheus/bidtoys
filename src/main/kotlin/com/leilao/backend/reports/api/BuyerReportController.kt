package com.leilao.backend.reports.api

import com.leilao.backend.reports.application.SubmitBuyerReportUseCase
import com.leilao.backend.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/buyer-reports")
@Tag(name = "Buyer Reports", description = "Reporte de compradores")
class BuyerReportController(
    private val submitBuyerReportUseCase: SubmitBuyerReportUseCase
) {

    @PostMapping("/{reportedUserId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia um reporte de comprador com motivo e imagens opcionais")
    fun submit(
        @PathVariable reportedUserId: UUID,
        @RequestParam("reason") reason: String,
        @RequestPart(value = "images", required = false) images: List<MultipartFile>?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Map<String, UUID> {
        val reportId = submitBuyerReportUseCase.execute(
            reporterId = principal.id,
            reportedUserId = reportedUserId,
            reason = reason,
            images = images ?: emptyList()
        )
        return mapOf("reportId" to reportId)
    }
}
