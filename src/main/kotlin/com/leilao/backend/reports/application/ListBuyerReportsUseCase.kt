package com.leilao.backend.reports.application

import com.leilao.backend.auctions.api.dto.BuyerAuctionItem
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.reports.api.dto.BuyerReportResponse
import com.leilao.backend.reports.infrastructure.BuyerReportRepository
import com.leilao.backend.users.api.dto.AddressResponse
import com.leilao.backend.users.infrastructure.UserAddressRepository
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListBuyerReportsUseCase(
    private val buyerReportRepository: BuyerReportRepository,
    private val userRepository: UserRepository,
    private val auctionRepository: AuctionRepository,
    private val userAddressRepository: UserAddressRepository
) {

    @Transactional(readOnly = true)
    fun execute(): List<BuyerReportResponse> {
        val reports = buyerReportRepository.findAllByOrderByCreatedAtDesc()

        return reports.map { report ->
            val buyer = userRepository.findById(report.reportedUserId).orElse(null)
            val address = userAddressRepository.findByUserId(report.reportedUserId).orElse(null)
            val auctions = auctionRepository.findByWinnerUserId(
                report.reportedUserId,
                PageRequest.of(0, 100)
            ).content

            BuyerReportResponse.from(
                report = report,
                reportedUserStatus = buyer?.status?.name ?: "UNKNOWN",
                buyerEmail = buyer?.email ?: "",
                buyerPhone = buyer?.phoneNumber,
                buyerAddress = address?.let { AddressResponse.from(it) },
                buyerAuctions = auctions.map { a ->
                    BuyerAuctionItem(
                        id = a.id,
                        title = a.title,
                        currentPriceAmount = a.currentPriceAmount,
                        status = a.status,
                        finishedAt = a.finishedAt,
                        holdShipment = a.holdShipment,
                        shipmentStatus = a.shipmentStatus,
                        trackingCode = a.trackingCode
                    )
                }
            )
        }
    }
}
