package com.leilao.backend.admin.api

import com.leilao.backend.admin.api.dto.RejectAuctionRequest
import com.leilao.backend.admin.application.ApproveAuctionUseCase
import com.leilao.backend.admin.application.ListPendingAuctionsUseCase
import com.leilao.backend.admin.application.RejectAuctionUseCase
import com.leilao.backend.auctions.api.dto.AuctionResponse
import com.leilao.backend.shared.api.PageResponse
import com.leilao.backend.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/auctions")
@Tag(name = "Admin - Auctions", description = "Operações administrativas sobre leilões")
class AdminAuctionController(
    private val approveAuctionUseCase: ApproveAuctionUseCase,
    private val rejectAuctionUseCase: RejectAuctionUseCase,
    private val listPendingAuctionsUseCase: ListPendingAuctionsUseCase
) {

    @GetMapping("/pending")
    @Operation(summary = "Lista leilões aguardando aprovação (PENDING_APPROVAL)")
    fun listPending(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): PageResponse<AuctionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"))
        return PageResponse.from(listPendingAuctionsUseCase.execute(pageable).map { AuctionResponse.from(it) })
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprova um leilão (PENDING_APPROVAL → READY_TO_START)")
    fun approve(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(approveAuctionUseCase.execute(id, principal.id))
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeita um leilão com motivo (PENDING_APPROVAL → REJECTED)")
    fun reject(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RejectAuctionRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(rejectAuctionUseCase.execute(id, principal.id, request.reason))
    }
}
