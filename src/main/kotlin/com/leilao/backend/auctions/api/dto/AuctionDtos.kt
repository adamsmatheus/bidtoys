package com.leilao.backend.auctions.api.dto

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionImage
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.companies.domain.Company
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class AuctionImageResponse(
    val id: UUID,
    val fileUrl: String,
    val position: Int
)

data class CompanyInfo(
    val id: UUID,
    val name: String,
    val logoUrl: String?
)

data class CreateAuctionRequest(

    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
    val title: String,

    val description: String? = null,

    @field:NotNull(message = "Preço inicial é obrigatório")
    @field:Min(value = 1, message = "Preço inicial deve ser pelo menos R$ 1")
    val initialPriceAmount: Int,

    @field:NotNull(message = "Incremento mínimo é obrigatório")
    @field:Min(value = 1, message = "Incremento mínimo deve ser pelo menos R$ 1")
    val minIncrementAmount: Int,

    @field:NotNull(message = "Duração é obrigatória")
    @field:Min(value = 60, message = "Duração mínima é 60 segundos")
    val durationSeconds: Int
)

data class UpdateAuctionRequest(

    @field:Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
    val title: String? = null,

    val description: String? = null,

    @field:Min(value = 1, message = "Preço inicial deve ser pelo menos R$ 1")
    val initialPriceAmount: Int? = null,

    @field:Min(value = 1, message = "Incremento mínimo deve ser pelo menos R$ 1")
    val minIncrementAmount: Int? = null,

    @field:Min(value = 60, message = "Duração mínima é 60 segundos")
    val durationSeconds: Int? = null
)

data class AuctionResponse(
    val id: UUID,
    val sellerId: UUID,
    val sellerName: String,
    val title: String,
    val description: String?,
    val initialPriceAmount: Int,
    val currentPriceAmount: Int,
    val minIncrementAmount: Int,
    val nextMinimumBid: Int,
    val durationSeconds: Int,
    val status: AuctionStatus,
    val startedAt: Instant?,
    val endsAt: Instant?,
    val leadingBidId: UUID?,
    val winnerUserId: UUID?,
    val finishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val images: List<AuctionImageResponse> = emptyList(),
    val company: CompanyInfo? = null
) {
    companion object {
        fun from(auction: Auction, images: List<AuctionImage> = emptyList(), company: Company? = null) = AuctionResponse(
            id = auction.id,
            sellerId = auction.seller.id,
            sellerName = auction.seller.name,
            title = auction.title,
            description = auction.description,
            initialPriceAmount = auction.initialPriceAmount,
            currentPriceAmount = auction.currentPriceAmount,
            minIncrementAmount = auction.minIncrementAmount,
            nextMinimumBid = auction.nextMinimumBid(),
            durationSeconds = auction.durationSeconds,
            status = auction.status,
            startedAt = auction.startedAt,
            endsAt = auction.endsAt,
            leadingBidId = auction.leadingBidId,
            winnerUserId = auction.winnerUserId,
            finishedAt = auction.finishedAt,
            createdAt = auction.createdAt,
            updatedAt = auction.updatedAt,
            images = images.map { AuctionImageResponse(it.id, it.fileUrl, it.position) },
            company = company?.let { CompanyInfo(it.id, it.name, it.logoUrl) }
        )
    }
}

data class CancelAuctionRequest(
    val reason: String? = null
)

data class RejectAuctionRequest(
    @field:NotBlank(message = "Motivo da rejeição é obrigatório")
    val reason: String
)
