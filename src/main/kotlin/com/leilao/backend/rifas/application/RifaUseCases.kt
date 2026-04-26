package com.leilao.backend.rifas.application

import com.leilao.backend.companies.infrastructure.CompanyRepository
import com.leilao.backend.rifas.api.dto.BuyTicketsRequest
import com.leilao.backend.rifas.api.dto.CreateRifaRequest
import com.leilao.backend.rifas.api.dto.DeclareWinnerRequest
import com.leilao.backend.rifas.api.dto.UpdateRifaRequest
import com.leilao.backend.rifas.domain.Rifa
import com.leilao.backend.rifas.domain.RifaStatus
import com.leilao.backend.rifas.domain.RifaTicket
import com.leilao.backend.rifas.infrastructure.RifaImageRepository
import com.leilao.backend.rifas.infrastructure.RifaRepository
import com.leilao.backend.rifas.infrastructure.RifaTicketRepository
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
class CreateRifaUseCase(
    private val rifaRepository: RifaRepository,
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository,
) {
    @Transactional
    fun execute(request: CreateRifaRequest, sellerId: UUID): Rifa {
        companyRepository.findByUserId(sellerId).orElseThrow {
            InvalidStateException("Cadastre sua empresa no perfil antes de criar uma rifa", "COMPANY_REQUIRED")
        }
        val seller = userRepository.findById(sellerId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        return rifaRepository.save(
            Rifa(
                seller = seller,
                title = request.title,
                description = request.description,
                ticketPriceAmount = request.ticketPriceAmount,
                totalTickets = request.totalTickets,
                drawDate = request.drawDate,
            )
        )
    }
}

@Service
class UpdateRifaUseCase(private val rifaRepository: RifaRepository) {
    @Transactional
    fun execute(rifaId: UUID, request: UpdateRifaRequest, userId: UUID): Rifa {
        val rifa = rifaRepository.findById(rifaId)
            .orElseThrow { NotFoundException("Rifa não encontrada") }

        if (!rifa.isOwnedBy(userId)) throw ForbiddenException("Você não tem permissão para editar esta rifa")
        if (!rifa.status.canEdit()) throw InvalidStateException("Rifa no estado ${rifa.status} não pode ser editada", "INVALID_STATE")

        request.title?.let { rifa.title = it }
        request.description?.let { rifa.description = it }
        request.ticketPriceAmount?.let { rifa.ticketPriceAmount = it }
        request.totalTickets?.let { rifa.totalTickets = it }
        request.drawDate?.let { rifa.drawDate = it }

        return rifaRepository.save(rifa)
    }
}

@Service
class GetRifaUseCase(
    private val rifaRepository: RifaRepository,
    private val rifaImageRepository: RifaImageRepository,
    private val rifaTicketRepository: RifaTicketRepository,
    private val companyRepository: CompanyRepository,
) {
    @Transactional(readOnly = true)
    fun execute(rifaId: UUID) = rifaRepository.findById(rifaId)
        .orElseThrow { NotFoundException("Rifa não encontrada") }
        .let { rifa ->
            Triple(
                rifa,
                rifaImageRepository.findByRifaIdOrderByPosition(rifaId),
                rifaTicketRepository.findByRifaIdOrderByTicketNumber(rifaId),
            )
        }

    fun findCompany(sellerId: UUID) = companyRepository.findByUserId(sellerId).orElse(null)
}

@Service
class ListRifasUseCase(
    private val rifaRepository: RifaRepository,
    private val rifaTicketRepository: RifaTicketRepository,
    private val rifaImageRepository: RifaImageRepository,
    private val companyRepository: CompanyRepository,
) {
    @Transactional(readOnly = true)
    fun execute(status: RifaStatus?, sellerId: UUID?, pageable: Pageable): Page<Rifa> {
        return when {
            sellerId != null && status != null -> rifaRepository.findBySellerIdAndStatus(sellerId, status, pageable)
            sellerId != null -> rifaRepository.findBySellerId(sellerId, pageable)
            status != null -> rifaRepository.findByStatus(status, pageable)
            else -> rifaRepository.findByStatusIn(
                listOf(RifaStatus.ACTIVE, RifaStatus.READY_TO_START, RifaStatus.FINISHED),
                pageable
            )
        }
    }

    fun soldTickets(rifaId: UUID) = rifaTicketRepository.countByRifaId(rifaId)
    fun images(rifaId: UUID) = rifaImageRepository.findByRifaIdOrderByPosition(rifaId)
    fun findCompany(sellerId: UUID) = companyRepository.findByUserId(sellerId).orElse(null)
}

@Service
class SubmitRifaUseCase(private val rifaRepository: RifaRepository) {
    @Transactional
    fun execute(rifaId: UUID, userId: UUID): Rifa {
        val rifa = rifaRepository.findById(rifaId).orElseThrow { NotFoundException("Rifa não encontrada") }
        if (!rifa.isOwnedBy(userId)) throw ForbiddenException("Você não tem permissão para enviar esta rifa")
        rifa.submitForApproval()
        return rifaRepository.save(rifa)
    }
}

@Service
class StartRifaUseCase(private val rifaRepository: RifaRepository) {
    @Transactional
    fun execute(rifaId: UUID, userId: UUID): Rifa {
        val rifa = rifaRepository.findById(rifaId).orElseThrow { NotFoundException("Rifa não encontrada") }
        if (!rifa.isOwnedBy(userId)) throw ForbiddenException("Você não tem permissão para iniciar esta rifa")
        rifa.start()
        return rifaRepository.save(rifa)
    }
}

@Service
class BuyTicketsUseCase(
    private val rifaRepository: RifaRepository,
    private val rifaTicketRepository: RifaTicketRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(rifaId: UUID, request: BuyTicketsRequest, buyerId: UUID): List<RifaTicket> {
        val rifa = rifaRepository.findById(rifaId).orElseThrow { NotFoundException("Rifa não encontrada") }
        if (!rifa.status.isActive()) throw InvalidStateException("Rifa não está ativa", "INVALID_STATE")

        val buyer = userRepository.findById(buyerId).orElseThrow { NotFoundException("Usuário não encontrado") }

        val tickets = request.ticketNumbers.map { number ->
            if (number < 1 || number > rifa.totalTickets)
                throw InvalidStateException("Número de bilhete inválido: $number", "INVALID_TICKET")
            if (rifaTicketRepository.existsByRifaIdAndTicketNumber(rifaId, number))
                throw InvalidStateException("Bilhete $number já está ocupado", "TICKET_TAKEN")

            rifaTicketRepository.save(RifaTicket(rifa = rifa, ticketNumber = number, buyer = buyer))
        }

        return tickets
    }
}

@Service
class DeclareWinnerUseCase(
    private val rifaRepository: RifaRepository,
    private val rifaTicketRepository: RifaTicketRepository,
) {
    @Transactional
    fun execute(rifaId: UUID, request: DeclareWinnerRequest, sellerId: UUID): Rifa {
        val rifa = rifaRepository.findById(rifaId).orElseThrow { NotFoundException("Rifa não encontrada") }

        val ticket = rifaTicketRepository.findByRifaIdAndTicketNumber(rifaId, request.winnerTicketNumber)
            .orElseThrow { InvalidStateException("Nenhum comprador para o bilhete ${request.winnerTicketNumber}", "NO_BUYER") }

        rifa.declareWinner(sellerId, request.winnerTicketNumber, ticket.buyer.id)
        return rifaRepository.save(rifa)
    }
}

@Service
class MyRifasUseCase(
    private val rifaRepository: RifaRepository,
    private val rifaTicketRepository: RifaTicketRepository,
    private val rifaImageRepository: RifaImageRepository,
) {
    @Transactional(readOnly = true)
    fun execute(sellerId: UUID, status: RifaStatus?, pageable: Pageable): Page<Rifa> {
        return if (status != null)
            rifaRepository.findBySellerIdAndStatus(sellerId, status, pageable)
        else
            rifaRepository.findBySellerId(sellerId, pageable)
    }

    fun soldTickets(rifaId: UUID) = rifaTicketRepository.countByRifaId(rifaId)
    fun images(rifaId: UUID) = rifaImageRepository.findByRifaIdOrderByPosition(rifaId)
}
