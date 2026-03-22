package com.leilao.backend.auctions.api

import com.leilao.backend.auctions.api.dto.AuctionResponse
import com.leilao.backend.auctions.api.dto.CancelAuctionRequest
import com.leilao.backend.auctions.api.dto.CreateAuctionRequest
import com.leilao.backend.auctions.api.dto.UpdateAuctionRequest
import com.leilao.backend.auctions.application.CancelAuctionUseCase
import com.leilao.backend.auctions.application.CreateAuctionUseCase
import com.leilao.backend.auctions.application.GetAuctionUseCase
import com.leilao.backend.auctions.application.ListAuctionsUseCase
import com.leilao.backend.auctions.application.StartAuctionUseCase
import com.leilao.backend.auctions.application.SubmitForApprovalUseCase
import com.leilao.backend.auctions.application.UpdateAuctionUseCase
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.shared.api.PageResponse
import com.leilao.backend.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/auctions")
@Tag(name = "Auctions", description = "Gerenciamento de leilões")
class AuctionController(
    private val createAuctionUseCase: CreateAuctionUseCase,
    private val updateAuctionUseCase: UpdateAuctionUseCase,
    private val submitForApprovalUseCase: SubmitForApprovalUseCase,
    private val startAuctionUseCase: StartAuctionUseCase,
    private val cancelAuctionUseCase: CancelAuctionUseCase,
    private val getAuctionUseCase: GetAuctionUseCase,
    private val listAuctionsUseCase: ListAuctionsUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo leilão (inicia como DRAFT)")
    fun create(
        @Valid @RequestBody request: CreateAuctionRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(createAuctionUseCase.execute(request, principal.id))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita um leilão (somente DRAFT ou REJECTED)")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAuctionRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(updateAuctionUseCase.execute(id, request, principal.id))
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Envia leilão para aprovação")
    fun submitForApproval(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(submitForApprovalUseCase.execute(id, principal.id))
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Inicia o leilão (somente READY_TO_START)")
    fun start(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(startAuctionUseCase.execute(id, principal.id))
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela o leilão (antes de iniciar)")
    fun cancel(
        @PathVariable id: UUID,
        @RequestBody(required = false) request: CancelAuctionRequest?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        return AuctionResponse.from(cancelAuctionUseCase.execute(id, principal.id, request?.reason))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhe de um leilão")
    fun getById(@PathVariable id: UUID): AuctionResponse {
        return AuctionResponse.from(getAuctionUseCase.execute(id))
    }

    @GetMapping
    @Operation(summary = "Lista leilões com paginação e filtro de status")
    fun list(
        @RequestParam(required = false) status: AuctionStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<AuctionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = listAuctionsUseCase.execute(status, pageable)
        return PageResponse.from(result.map { AuctionResponse.from(it) })
    }
}
