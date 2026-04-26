package com.leilao.backend.rifas.api

import com.leilao.backend.rifas.api.dto.BuyTicketsRequest
import com.leilao.backend.rifas.api.dto.CreateRifaRequest
import com.leilao.backend.rifas.api.dto.DeclareWinnerRequest
import com.leilao.backend.rifas.api.dto.RejectRifaRequest
import com.leilao.backend.rifas.api.dto.RifaResponse
import com.leilao.backend.rifas.api.dto.UpdateRifaRequest
import com.leilao.backend.rifas.application.BuyTicketsUseCase
import com.leilao.backend.rifas.application.CreateRifaUseCase
import com.leilao.backend.rifas.application.DeclareWinnerUseCase
import com.leilao.backend.rifas.application.GetRifaUseCase
import com.leilao.backend.rifas.application.ListRifasUseCase
import com.leilao.backend.rifas.application.MyRifasUseCase
import com.leilao.backend.rifas.application.StartRifaUseCase
import com.leilao.backend.rifas.application.SubmitRifaUseCase
import com.leilao.backend.rifas.application.UpdateRifaUseCase
import com.leilao.backend.rifas.domain.RifaStatus
import com.leilao.backend.shared.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
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
@RequestMapping("/api/rifas")
class RifaController(
    private val createRifaUseCase: CreateRifaUseCase,
    private val updateRifaUseCase: UpdateRifaUseCase,
    private val getRifaUseCase: GetRifaUseCase,
    private val listRifasUseCase: ListRifasUseCase,
    private val submitRifaUseCase: SubmitRifaUseCase,
    private val startRifaUseCase: StartRifaUseCase,
    private val buyTicketsUseCase: BuyTicketsUseCase,
    private val declareWinnerUseCase: DeclareWinnerUseCase,
    private val myRifasUseCase: MyRifasUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) status: RifaStatus?,
        @RequestParam(required = false) sellerId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
    ): Page<RifaResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val rifas = listRifasUseCase.execute(status, sellerId, pageable)
        return rifas.map { rifa ->
            RifaResponse.from(
                rifa = rifa,
                soldTickets = listRifasUseCase.soldTickets(rifa.id),
                images = listRifasUseCase.images(rifa.id),
                company = listRifasUseCase.findCompany(rifa.seller.id),
            )
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): RifaResponse {
        val (rifa, images, tickets) = getRifaUseCase.execute(id)
        return RifaResponse.from(
            rifa = rifa,
            soldTickets = tickets.size.toLong(),
            images = images,
            company = getRifaUseCase.findCompany(rifa.seller.id),
            tickets = tickets,
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateRifaRequest,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): RifaResponse {
        val rifa = createRifaUseCase.execute(request, principal.userId)
        return RifaResponse.from(rifa)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRifaRequest,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): RifaResponse {
        val rifa = updateRifaUseCase.execute(id, request, principal.userId)
        return RifaResponse.from(rifa)
    }

    @PostMapping("/{id}/submit")
    fun submit(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): RifaResponse {
        val rifa = submitRifaUseCase.execute(id, principal.userId)
        return RifaResponse.from(rifa)
    }

    @PostMapping("/{id}/start")
    fun start(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): RifaResponse {
        val rifa = startRifaUseCase.execute(id, principal.userId)
        return RifaResponse.from(rifa)
    }

    @PostMapping("/{id}/buy")
    fun buyTickets(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BuyTicketsRequest,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): RifaResponse {
        buyTicketsUseCase.execute(id, request, principal.userId)
        val (rifa, images, tickets) = getRifaUseCase.execute(id)
        return RifaResponse.from(rifa, tickets.size.toLong(), images, getRifaUseCase.findCompany(rifa.seller.id), tickets)
    }

    @PostMapping("/{id}/winner")
    fun declareWinner(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DeclareWinnerRequest,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): RifaResponse {
        val rifa = declareWinnerUseCase.execute(id, request, principal.userId)
        return RifaResponse.from(rifa)
    }

    @GetMapping("/my-rifas")
    fun myRifas(
        @RequestParam(required = false) status: RifaStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): Page<RifaResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val rifas = myRifasUseCase.execute(principal.userId, status, pageable)
        return rifas.map { rifa ->
            RifaResponse.from(
                rifa = rifa,
                soldTickets = myRifasUseCase.soldTickets(rifa.id),
                images = myRifasUseCase.images(rifa.id),
            )
        }
    }
}
