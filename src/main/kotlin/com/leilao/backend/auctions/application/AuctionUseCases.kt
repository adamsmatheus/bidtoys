package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.api.dto.CreateAuctionRequest
import com.leilao.backend.auctions.api.dto.UpdateAuctionRequest
import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.domain.AuctionStatusHistory
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.AuctionStatusHistoryRepository
import com.leilao.backend.companies.infrastructure.CompanyRepository
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository
) {
    @Transactional
    fun execute(request: CreateAuctionRequest, sellerId: UUID): Auction {
        companyRepository.findByUserId(sellerId).orElseThrow {
            InvalidStateException(
                "Cadastre sua empresa no perfil antes de criar um leilão",
                "COMPANY_REQUIRED"
            )
        }

        val seller = userRepository.findById(sellerId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val auction = Auction(
            seller = seller,
            title = request.title,
            description = request.description,
            initialPriceAmount = request.initialPriceAmount,
            minIncrementAmount = request.minIncrementAmount,
            durationSeconds = request.durationSeconds
        )

        val saved = auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = saved,
                fromStatus = null,
                toStatus = AuctionStatus.DRAFT,
                changedByUserId = sellerId
            )
        )

        return saved
    }
}

@Service
class UpdateAuctionUseCase(
    private val auctionRepository: AuctionRepository
) {
    @Transactional
    fun execute(auctionId: UUID, request: UpdateAuctionRequest, userId: UUID): Auction {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para editar este leilão")
        }

        if (!auction.status.canEdit()) {
            throw InvalidStateException(
                "Leilão no estado ${auction.status} não pode ser editado",
                "INVALID_STATE_FOR_EDIT"
            )
        }

        request.title?.let { auction.title = it }
        request.description?.let { auction.description = it }
        request.initialPriceAmount?.let { auction.updateInitialPrice(it) }
        request.minIncrementAmount?.let { auction.minIncrementAmount = it }
        request.durationSeconds?.let { auction.durationSeconds = it }

        return auctionRepository.save(auction)
    }
}

@Service
class SubmitForApprovalUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository
) {
    @Transactional
    fun execute(auctionId: UUID, userId: UUID): Auction {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para enviar este leilão para aprovação")
        }

        val fromStatus = auction.status
        auction.submitForApproval()
        val saved = auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = saved,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.PENDING_APPROVAL,
                changedByUserId = userId
            )
        )

        return saved
    }
}

@Service
class StartAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository
) {
    @Transactional
    fun execute(auctionId: UUID, userId: UUID): Auction {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para iniciar este leilão")
        }

        val fromStatus = auction.status
        auction.start()
        val saved = auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = saved,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.ACTIVE,
                changedByUserId = userId
            )
        )

        return saved
    }
}

@Service
class CancelAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository
) {
    @Transactional
    fun execute(auctionId: UUID, userId: UUID, reason: String?): Auction {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para cancelar este leilão")
        }

        val fromStatus = auction.status
        auction.cancel(userId, reason)
        val saved = auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = saved,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.CANCELLED,
                changedByUserId = userId,
                reason = reason
            )
        )

        return saved
    }
}

@Service
class GetAuctionUseCase(
    private val auctionRepository: AuctionRepository
) {
    @Transactional(readOnly = true)
    fun execute(auctionId: UUID): Auction {
        return auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }
    }
}

private val PUBLIC_STATUSES = setOf(
    AuctionStatus.READY_TO_START,
    AuctionStatus.ACTIVE
)

@Service
class ListWonAuctionsUseCase(
    private val auctionRepository: AuctionRepository
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID, pageable: Pageable): Page<Auction> {
        return auctionRepository.findByWinnerUserId(userId, pageable)
    }
}

@Service
class ListAuctionsUseCase(
    private val auctionRepository: AuctionRepository
) {
    @Transactional(readOnly = true)
    fun execute(status: AuctionStatus?, sellerId: UUID?, requesterId: UUID?, pageable: Pageable): Page<Auction> {
        val isOwner = requesterId != null && requesterId == sellerId

        return when {
            // Dono vendo seus próprios leilões — todos os statuses permitidos
            isOwner && status != null -> auctionRepository.findBySellerIdAndStatus(sellerId!!, status, pageable)
            isOwner -> auctionRepository.findBySellerId(sellerId!!, pageable)

            // Público vendo leilões de um vendedor — apenas statuses públicos
            sellerId != null && status != null -> {
                if (status in PUBLIC_STATUSES) auctionRepository.findBySellerIdAndStatus(sellerId, status, pageable)
                else Page.empty(pageable)
            }
            sellerId != null -> auctionRepository.findBySellerIdAndStatusIn(sellerId, PUBLIC_STATUSES, pageable)

            // Listagem geral — apenas statuses públicos
            status != null -> {
                if (status in PUBLIC_STATUSES) auctionRepository.findByStatus(status, pageable)
                else Page.empty(pageable)
            }
            else -> auctionRepository.findByStatusIn(PUBLIC_STATUSES, pageable)
        }
    }
}
