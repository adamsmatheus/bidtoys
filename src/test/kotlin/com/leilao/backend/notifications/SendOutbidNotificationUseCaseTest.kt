package com.leilao.backend.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.application.OutbidNotificationCommand
import com.leilao.backend.notifications.application.SendOutbidNotificationUseCase
import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationChannel
import com.leilao.backend.notifications.domain.NotificationStatus
import com.leilao.backend.notifications.domain.NotificationType
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionGateway
import com.leilao.backend.notifications.infrastructure.whatsapp.EvolutionSendException
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import com.leilao.backend.users.infrastructure.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class SendOutbidNotificationUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val notificationRepository = mockk<NotificationRepository>()
    private val whatsAppGateway = mockk<EvolutionGateway>()
    private val objectMapper = ObjectMapper()

    private val useCase = SendOutbidNotificationUseCase(
        userRepository, notificationRepository, whatsAppGateway, objectMapper
    )

    private val userId = UUID.randomUUID()
    private val auctionId = UUID.randomUUID()

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        user = User(
            name = "João Silva",
            email = "joao@test.com",
            passwordHash = "hash",
            phoneNumber = "11999990000",
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )

        val notificationSlot = slot<Notification>()
        every { notificationRepository.save(capture(notificationSlot)) } answers { notificationSlot.captured }
    }

    @Test
    fun `deve enviar notificação WhatsApp quando lance é superado`() {
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { whatsAppGateway.sendOutbidNotification(any(), any(), any(), any()) } returns Unit

        val command = OutbidNotificationCommand(
            auctionId = auctionId,
            outbidUserId = userId,
            auctionTitle = "Funko Pop Batman",
            newAmount = 150
        )

        useCase.execute(command)

        verify(exactly = 1) {
            whatsAppGateway.sendOutbidNotification(
                phoneNumber = user.phoneNumber,
                name = user.name,
                auctionTitle = "Funko Pop Batman",
                newAmount = "150,00"
            )
        }
    }

    @Test
    fun `deve formatar valor corretamente`() {
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { whatsAppGateway.sendOutbidNotification(any(), any(), any(), any()) } returns Unit

        val command = OutbidNotificationCommand(
            auctionId = auctionId,
            outbidUserId = userId,
            auctionTitle = "Leilão Teste",
            newAmount = 1200
        )

        useCase.execute(command)

        verify {
            whatsAppGateway.sendOutbidNotification(
                phoneNumber = any(),
                name = any(),
                auctionTitle = any(),
                newAmount = "1200,00"
            )
        }
    }

    @Test
    fun `deve ignorar silenciosamente quando usuário não existe`() {
        every { userRepository.findById(userId) } returns Optional.empty()

        val command = OutbidNotificationCommand(
            auctionId = auctionId,
            outbidUserId = userId,
            auctionTitle = "Leilão Teste",
            newAmount = 100
        )

        useCase.execute(command)

        verify(exactly = 0) { whatsAppGateway.sendOutbidNotification(any(), any(), any(), any()) }
        verify(exactly = 0) { notificationRepository.save(any()) }
    }

    @Test
    fun `deve marcar notificação como falha quando WhatsApp lança exceção`() {
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { whatsAppGateway.sendOutbidNotification(any(), any(), any(), any()) } throws
            EvolutionSendException("Erro de envio")

        val savedNotifications = mutableListOf<Notification>()
        every { notificationRepository.save(any()) } answers {
            val n = firstArg<Notification>()
            savedNotifications.add(n)
            n
        }

        val command = OutbidNotificationCommand(
            auctionId = auctionId,
            outbidUserId = userId,
            auctionTitle = "Leilão Teste",
            newAmount = 100
        )

        useCase.execute(command)

        val failedNotification = savedNotifications.last()
        assertEquals(NotificationStatus.FAILED, failedNotification.status)
        assertEquals(NotificationType.AUCTION_OUTBID, failedNotification.type)
        assertEquals(NotificationChannel.WHATSAPP, failedNotification.channel)
    }
}
