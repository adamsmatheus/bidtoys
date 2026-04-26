package com.leilao.backend.rifas.api.dto

import com.leilao.backend.auctions.api.dto.CompanyInfo
import com.leilao.backend.rifas.domain.Rifa
import com.leilao.backend.rifas.domain.RifaImage
import com.leilao.backend.rifas.domain.RifaStatus
import com.leilao.backend.rifas.domain.RifaTicket
import com.leilao.backend.companies.domain.Company
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateRifaRequest(

    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
    val title: String,

    val description: String? = null,

    @field:NotNull(message = "Preço do bilhete é obrigatório")
    @field:Min(value = 1, message = "Preço do bilhete deve ser pelo menos R$ 0,01")
    val ticketPriceAmount: Int,

    @field:NotNull(message = "Total de bilhetes é obrigatório")
    @field:Min(value = 2, message = "A rifa deve ter pelo menos 2 bilhetes")
    val totalTickets: Int,

    @field:NotNull(message = "Data do sorteio é obrigatória")
    @field:Future(message = "A data do sorteio deve ser no futuro")
    val drawDate: Instant,
)

data class UpdateRifaRequest(
    @field:Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
    val title: String? = null,

    val description: String? = null,

    @field:Min(value = 1, message = "Preço do bilhete deve ser pelo menos R$ 0,01")
    val ticketPriceAmount: Int? = null,

    @field:Min(value = 2, message = "A rifa deve ter pelo menos 2 bilhetes")
    val totalTickets: Int? = null,

    val drawDate: Instant? = null,
)

data class RifaImageResponse(
    val id: UUID,
    val fileUrl: String,
    val position: Int,
)

data class RifaTicketResponse(
    val ticketNumber: Int,
    val buyerId: UUID?,
    val buyerName: String?,
    val purchasedAt: Instant?,
)

data class RifaResponse(
    val id: UUID,
    val sellerId: UUID,
    val sellerName: String,
    val title: String,
    val description: String?,
    val ticketPriceAmount: Int,
    val totalTickets: Int,
    val soldTickets: Long,
    val drawDate: Instant,
    val status: RifaStatus,
    val winnerTicketNumber: Int?,
    val winnerUserId: UUID?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val images: List<RifaImageResponse> = emptyList(),
    val company: CompanyInfo? = null,
    val tickets: List<RifaTicketResponse> = emptyList(),
) {
    companion object {
        fun from(
            rifa: Rifa,
            soldTickets: Long = 0,
            images: List<RifaImage> = emptyList(),
            company: Company? = null,
            tickets: List<RifaTicket> = emptyList(),
        ) = RifaResponse(
            id = rifa.id,
            sellerId = rifa.seller.id,
            sellerName = rifa.seller.name,
            title = rifa.title,
            description = rifa.description,
            ticketPriceAmount = rifa.ticketPriceAmount,
            totalTickets = rifa.totalTickets,
            soldTickets = soldTickets,
            drawDate = rifa.drawDate,
            status = rifa.status,
            winnerTicketNumber = rifa.winnerTicketNumber,
            winnerUserId = rifa.winnerUserId,
            startedAt = rifa.startedAt,
            finishedAt = rifa.finishedAt,
            createdAt = rifa.createdAt,
            updatedAt = rifa.updatedAt,
            images = images.map { RifaImageResponse(it.id, it.fileUrl, it.position) },
            company = company?.let { CompanyInfo(it.id, it.name, it.logoUrl, it.pixKey) },
            tickets = tickets.map {
                RifaTicketResponse(it.ticketNumber, it.buyer.id, it.buyer.name, it.createdAt)
            },
        )
    }
}

data class BuyTicketsRequest(
    @field:NotNull
    val ticketNumbers: List<Int>,
)

data class DeclareWinnerRequest(
    @field:NotNull(message = "Número do bilhete vencedor é obrigatório")
    @field:Min(value = 1, message = "Número do bilhete deve ser pelo menos 1")
    val winnerTicketNumber: Int,
)

data class RejectRifaRequest(
    @field:NotBlank(message = "Motivo da rejeição é obrigatório")
    val reason: String,
)
