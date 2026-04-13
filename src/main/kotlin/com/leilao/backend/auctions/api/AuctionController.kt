package com.leilao.backend.auctions.api

import com.leilao.backend.auctions.api.dto.AuctionImageResponse
import com.leilao.backend.auctions.api.dto.AuctionResponse
import com.leilao.backend.auctions.api.dto.CancelAuctionRequest
import com.leilao.backend.auctions.api.dto.CreateAuctionRequest
import com.leilao.backend.auctions.api.dto.UpdateAuctionRequest
import com.leilao.backend.auctions.application.CancelAuctionUseCase
import com.leilao.backend.auctions.application.ConfirmPaymentUseCase
import com.leilao.backend.auctions.application.CreateAuctionUseCase
import com.leilao.backend.auctions.application.DeclarePaymentUseCase
import com.leilao.backend.auctions.application.DeleteAuctionImageUseCase
import com.leilao.backend.auctions.application.GetAuctionUseCase
import com.leilao.backend.auctions.application.ListAuctionsUseCase
import com.leilao.backend.auctions.application.ListWonAuctionsUseCase
import com.leilao.backend.auctions.application.StartAuctionUseCase
import com.leilao.backend.auctions.application.SubmitForApprovalUseCase
import com.leilao.backend.auctions.application.UpdateAuctionUseCase
import com.leilao.backend.auctions.application.UploadAuctionImageUseCase
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.infrastructure.AuctionImageRepository
import com.leilao.backend.bids.infrastructure.BidRepository
import com.leilao.backend.companies.infrastructure.CompanyRepository
import com.leilao.backend.shared.api.PageResponse
import com.leilao.backend.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
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
    private val declarePaymentUseCase: DeclarePaymentUseCase,
    private val confirmPaymentUseCase: ConfirmPaymentUseCase,
    private val getAuctionUseCase: GetAuctionUseCase,
    private val listAuctionsUseCase: ListAuctionsUseCase,
    private val listWonAuctionsUseCase: ListWonAuctionsUseCase,
    private val uploadAuctionImageUseCase: UploadAuctionImageUseCase,
    private val deleteAuctionImageUseCase: DeleteAuctionImageUseCase,
    private val auctionImageRepository: AuctionImageRepository,
    private val bidRepository: BidRepository,
    private val companyRepository: CompanyRepository
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo leilão (inicia como DRAFT)")
    fun create(
        @Valid @RequestBody request: CreateAuctionRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        val auction = createAuctionUseCase.execute(request, principal.id)
        val company = companyRepository.findByUserId(principal.id).orElse(null)
        return AuctionResponse.from(auction, company = company)
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

    @PostMapping("/{id}/declare-payment")
    @Operation(summary = "Vencedor declara que realizou o pagamento via PIX")
    fun declarePayment(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        declarePaymentUseCase.execute(id, principal.id)
        val auction = getAuctionUseCase.execute(id)
        val company = companyRepository.findByUserId(auction.seller.id).orElse(null)
        return AuctionResponse.from(auction, company = company)
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Vendedor confirma o recebimento do pagamento")
    fun confirmPayment(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        confirmPaymentUseCase.execute(id, principal.id, confirmed = true)
        val auction = getAuctionUseCase.execute(id)
        val company = companyRepository.findByUserId(auction.seller.id).orElse(null)
        return AuctionResponse.from(auction, company = company)
    }

    @PostMapping("/{id}/dispute-payment")
    @Operation(summary = "Vendedor contesta o pagamento declarado pelo vencedor")
    fun disputePayment(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionResponse {
        confirmPaymentUseCase.execute(id, principal.id, confirmed = false)
        val auction = getAuctionUseCase.execute(id)
        val company = companyRepository.findByUserId(auction.seller.id).orElse(null)
        return AuctionResponse.from(auction, company = company)
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
        val auction = getAuctionUseCase.execute(id)
        val images = auctionImageRepository.findByAuction_IdOrderByPositionAsc(auction.id)
        val company = companyRepository.findByUserId(auction.seller.id).orElse(null)
        return AuctionResponse.from(auction, images, company)
    }

    private fun bidCountMap(auctionIds: List<UUID>): Map<UUID, Long> =
        auctionIds.associateWith { bidRepository.countByAuction_Id(it) }

    @GetMapping("/won")
    @Operation(summary = "Lista os leilões arrematados pelo usuário autenticado")
    fun listWon(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal principal: UserPrincipal
    ): PageResponse<AuctionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "finishedAt"))
        val result = listWonAuctionsUseCase.execute(principal.id, pageable)
        val auctionIds = result.content.map { it.id }
        val sellerIds = result.content.map { it.seller.id }.distinct()
        val companyMap = companyRepository.findByUserIdIn(sellerIds).associateBy { it.user.id }
        val imagesMap = auctionImageRepository.findByAuction_IdInOrderByPositionAsc(auctionIds)
            .groupBy { it.auction.id }
        val bidCounts = bidCountMap(auctionIds)
        return PageResponse.from(result.map {
            AuctionResponse.from(it, images = imagesMap[it.id] ?: emptyList(), company = companyMap[it.seller.id], bidCount = bidCounts[it.id] ?: 0)
        })
    }

    @GetMapping
    @Operation(summary = "Lista leilões com paginação, filtro de status e vendedor")
    fun list(
        @RequestParam(required = false) status: AuctionStatus?,
        @RequestParam(required = false) sellerId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal principal: UserPrincipal?
    ): PageResponse<AuctionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = listAuctionsUseCase.execute(status, sellerId, principal?.id, pageable)
        val auctionIds = result.content.map { it.id }
        val sellerIds = result.content.map { it.seller.id }.distinct()
        val companyMap = companyRepository.findByUserIdIn(sellerIds).associateBy { it.user.id }
        val imagesMap = auctionImageRepository.findByAuction_IdInOrderByPositionAsc(auctionIds)
            .groupBy { it.auction.id }
        val bidCounts = bidCountMap(auctionIds)
        return PageResponse.from(result.map {
            AuctionResponse.from(it, images = imagesMap[it.id] ?: emptyList(), company = companyMap[it.seller.id], bidCount = bidCounts[it.id] ?: 0)
        })
    }

    @PostMapping("/{id}/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona uma foto ao leilão (máx. 5, somente DRAFT ou REJECTED)")
    fun uploadImage(
        @PathVariable id: UUID,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal principal: UserPrincipal
    ): AuctionImageResponse {
        return uploadAuctionImageUseCase.execute(id, principal.id, file)
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma foto do leilão (somente DRAFT ou REJECTED)")
    fun deleteImage(
        @PathVariable id: UUID,
        @PathVariable imageId: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ) {
        deleteAuctionImageUseCase.execute(id, imageId, principal.id)
    }
}
