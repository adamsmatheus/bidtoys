package com.leilao.backend.auctions

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AuctionDomainTest {

    private fun createSeller() = User(
        name = "Vendedor",
        email = "seller@test.com",
        passwordHash = "hash",
        role = UserRole.USER,
        status = UserStatus.ACTIVE
    )

    private fun createAuction(seller: User = createSeller()) = Auction(
        seller = seller,
        title = "Produto Teste",
        initialPriceAmount = 500,
        minIncrementAmount = 50,
        durationSeconds = 3600
    )

    @Test
    fun `leilão criado deve ter status DRAFT`() {
        val auction = createAuction()
        assertEquals(AuctionStatus.DRAFT, auction.status)
        assertEquals(500, auction.currentPriceAmount)
        assertEquals(550, auction.nextMinimumBid())
    }

    @Test
    fun `deve fazer transição DRAFT para PENDING_APPROVAL`() {
        val auction = createAuction()
        auction.submitForApproval()
        assertEquals(AuctionStatus.PENDING_APPROVAL, auction.status)
    }

    @Test
    fun `deve aprovar leilão`() {
        val adminId = UUID.randomUUID()
        val auction = createAuction()
        auction.submitForApproval()
        auction.approve(adminId)
        assertEquals(AuctionStatus.READY_TO_START, auction.status)
        assertEquals(adminId, auction.approvedByUserId)
        assertNotNull(auction.approvedAt)
    }

    @Test
    fun `deve rejeitar leilão com motivo`() {
        val adminId = UUID.randomUUID()
        val auction = createAuction()
        auction.submitForApproval()
        auction.reject(adminId, "Fotos inadequadas")
        assertEquals(AuctionStatus.REJECTED, auction.status)
        assertEquals("Fotos inadequadas", auction.rejectionReason)
    }

    @Test
    fun `deve iniciar leilão e calcular ends_at`() {
        val auction = createAuction()
        auction.submitForApproval()
        auction.approve(UUID.randomUUID())
        auction.start()
        assertEquals(AuctionStatus.ACTIVE, auction.status)
        assertNotNull(auction.startedAt)
        assertNotNull(auction.endsAt)
    }

    @Test
    fun `deve prorrogar leilão quando lance cai no último minuto`() {
        val auction = createAuction()
        auction.submitForApproval()
        auction.approve(UUID.randomUUID())
        auction.start()

        // Simular ends_at em 30 segundos (dentro do último minuto)
        val endsAtField = Auction::class.java.getDeclaredField("endsAt")
        endsAtField.isAccessible = true
        val nearEnd = java.time.Instant.now().plusSeconds(30)
        endsAtField.set(auction, nearEnd)

        val bidId = UUID.randomUUID()
        auction.receiveNewBid(bidId, 550)

        // Após prorrogação, ends_at deve ser ~2 minutos a partir de agora
        val newEndsAt = auction.endsAt!!
        assertTrue(newEndsAt.isAfter(java.time.Instant.now().plusSeconds(90)))
    }

    @Test
    fun `não deve permitir cancelar leilão ativo`() {
        val auction = createAuction()
        auction.submitForApproval()
        auction.approve(UUID.randomUUID())
        auction.start()

        assertThrows<IllegalStateException> {
            auction.cancel(UUID.randomUUID())
        }
    }

    @Test
    fun `não deve permitir aprovar leilão não pendente`() {
        val auction = createAuction()
        // Status ainda é DRAFT

        assertThrows<IllegalStateException> {
            auction.approve(UUID.randomUUID())
        }
    }
}
