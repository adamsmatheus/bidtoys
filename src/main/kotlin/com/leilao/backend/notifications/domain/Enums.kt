package com.leilao.backend.notifications.domain

enum class NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED
}

enum class NotificationType {
    WINNER_NOTIFICATION,
    AUCTION_STARTING_SOON,
    AUCTION_OUTBID,
    ADMIN_ALERT
}

enum class NotificationChannel {
    WHATSAPP,
    EMAIL,
    PUSH
}
