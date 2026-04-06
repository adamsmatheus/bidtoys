package com.leilao.backend.bids.api.dto

import com.leilao.backend.bids.domain.Bid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class PlaceBidRequest(

    @field:NotNull(message = "Valor do lance é obrigatório")
    @field:Min(value = 1, message = "Valor do lance deve ser positivo")
    val amount: Int,

    /**
     * ID opcional de idempotência enviado pelo cliente.
     * Se repetido, retorna o lance já registrado sem criar um novo.
     */
    val requestId: String? = null
)

data class BidResponse(
    val id: UUID,
    val auctionId: UUID,
    val bidderId: UUID,
    val bidderName: String,
    val amount: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(bid: Bid) = BidResponse(
            id = bid.id,
            auctionId = bid.auctionId,
            bidderId = bid.bidderId,
            bidderName = bid.bidder.name,
            amount = bid.amount,
            createdAt = bid.createdAt
        )
    }
}
