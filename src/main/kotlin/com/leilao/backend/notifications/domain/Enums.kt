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
    ADMIN_ALERT,
    PAYMENT_DECLARED,
    PAYMENT_CONFIRMED
}

enum class NotificationChannel {
    WHATSAPP,
    EMAIL,
    PUSH
}
