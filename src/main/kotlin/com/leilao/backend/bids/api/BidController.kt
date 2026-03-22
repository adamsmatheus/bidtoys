package com.leilao.backend.bids.api

import com.leilao.backend.bids.api.dto.BidResponse
import com.leilao.backend.bids.api.dto.PlaceBidRequest
import com.leilao.backend.bids.application.PlaceBidUseCase
import com.leilao.backend.bids.infrastructure.BidRepository
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@Tag(name = "Bids", description = "Lances em leilões")
class BidController(
    private val placeBidUseCase: PlaceBidUseCase,
    private val bidRepository: BidRepository
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um lance no leilão")
    fun placeBid(
        @PathVariable auctionId: UUID,
        @Valid @RequestBody request: PlaceBidRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): BidResponse {
        val bid = placeBidUseCase.execute(auctionId, request, principal.id)
        return BidResponse.from(bid)
    }

    @GetMapping
    @Operation(summary = "Lista os lances de um leilão")
    fun listBids(
        @PathVariable auctionId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<BidResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId, pageable)
        return PageResponse.from(result.map { BidResponse.from(it) })
    }
}
