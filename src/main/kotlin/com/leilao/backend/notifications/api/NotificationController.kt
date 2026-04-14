package com.leilao.backend.notifications.api

import com.leilao.backend.notifications.infrastructure.InAppNotificationRepository
import com.leilao.backend.shared.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class InAppNotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val auctionId: String?,
    val read: Boolean,
    val createdAt: Instant
)

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val repository: InAppNotificationRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun list(@AuthenticationPrincipal principal: UserPrincipal): List<InAppNotificationResponse> {
        return repository.findTop50ByUserIdOrderByCreatedAtDesc(principal.id)
            .map {
                InAppNotificationResponse(
                    id = it.id.toString(),
                    type = it.type,
                    title = it.title,
                    message = it.message,
                    auctionId = it.auctionId?.toString(),
                    read = it.read,
                    createdAt = it.createdAt
                )
            }
    }

    @PatchMapping("/{id}/read")
    @Transactional
    fun markRead(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        repository.findById(id).ifPresent { notification ->
            if (notification.userId == principal.id) {
                notification.read = true
                repository.save(notification)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/read-all")
    @Transactional
    fun markAllRead(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<Void> {
        repository.markAllAsReadByUserId(principal.id)
        return ResponseEntity.noContent().build()
    }
}
