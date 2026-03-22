package com.leilao.backend.bids

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.bids.api.dto.PlaceBidRequest
import com.leilao.backend.bids.application.PlaceBidUseCase
import com.leilao.backend.bids.domain.Bid
import com.leilao.backend.bids.infrastructure.BidRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import com.leilao.backend.users.infrastructure.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Optional
import java.util.UUID

class PlaceBidUseCaseTest {

    private val auctionRepository = mockk<AuctionRepository>()
    private val bidRepository = mockk<BidRepository>()
    private val userRepository = mockk<UserRepository>()

    private val useCase = PlaceBidUseCase(auctionRepository, bidRepository, userRepository)

    private val sellerId = UUID.randomUUID()
    private val bidderId = UUID.randomUUID()

    private lateinit var seller: User
    private lateinit var bidder: User
    private lateinit var activeAuction: Auction

    @BeforeEach
    fun setup() {
        seller = User(
            name = "Vendedor",
            email = "seller@test.com",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )

        bidder = User(
            name = "Licitante",
            email = "bidder@test.com",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )

        activeAuction = Auction(
            seller = seller,
            title = "Produto Teste",
            initialPriceAmount = 100,
            minIncrementAmount = 10,
            durationSeconds = 3600
        )

        // Usar reflection para setar status como ACTIVE com ends_at futuro
        val statusField = Auction::class.java.getDeclaredField("status")
        statusField.isAccessible = true
        statusField.set(activeAuction, AuctionStatus.ACTIVE)

        val endsAtField = Auction::class.java.getDeclaredField("endsAt")
        endsAtField.isAccessible = true
        endsAtField.set(activeAuction, Instant.now().plusSeconds(3600))
    }

    @Test
    fun `deve aceitar lance válido`() {
        val request = PlaceBidRequest(amount = 110)
        val auctionId = activeAuction.id

        every { bidRepository.findByRequestId(any()) } returns Optional.empty()
        every { auctionRepository.findByIdWithLock(auctionId) } returns Optional.of(activeAuction)
        every { userRepository.findById(bidderId) } returns Optional.of(bidder)
        every { bidRepository.save(any()) } answers { firstArg() }
        every { auctionRepository.save(any()) } returns activeAuction

        val result = useCase.execute(auctionId, request, bidderId)

        assertEquals(110, result.amount)
        verify { auctionRepository.save(activeAuction) }
    }

    @Test
    fun `deve rejeitar lance abaixo do mínimo`() {
        val request = PlaceBidRequest(amount = 105) // mínimo é 100 + 10 = 110
        val auctionId = activeAuction.id

        every { bidRepository.findByRequestId(any()) } returns Optional.empty()
        every { auctionRepository.findByIdWithLock(auctionId) } returns Optional.of(activeAuction)

        assertThrows<BusinessException> {
            useCase.execute(auctionId, request, bidderId)
        }
    }

    @Test
    fun `deve rejeitar lance do próprio dono`() {
        val request = PlaceBidRequest(amount = 110)
        val auctionId = activeAuction.id

        every { bidRepository.findByRequestId(any()) } returns Optional.empty()
        every { auctionRepository.findByIdWithLock(auctionId) } returns Optional.of(activeAuction)

        // sellerId é o dono — usando o id do seller que foi gerado no domínio
        assertThrows<ForbiddenException> {
            useCase.execute(auctionId, request, seller.id)
        }
    }

    @Test
    fun `deve retornar lance existente para requestId duplicado`() {
        val requestId = "idem-123"
        val existingBid = mockk<Bid> {
            every { amount } returns 110
            every { bidderId } returns this@PlaceBidUseCaseTest.bidderId
        }

        every { bidRepository.findByRequestId(requestId) } returns Optional.of(existingBid)

        val result = useCase.execute(activeAuction.id, PlaceBidRequest(110, requestId), bidderId)

        assertEquals(existingBid, result)
        verify(exactly = 0) { auctionRepository.findByIdWithLock(any()) }
    }
}
