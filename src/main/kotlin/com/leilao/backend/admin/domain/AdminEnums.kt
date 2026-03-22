package com.leilao.backend.admin.domain

enum class AdminAlertStatus {
    OPEN,
    RESOLVED
}

enum class AdminAlertType {
    WHATSAPP_FAILED,
    AUCTION_MISMATCH,
    WORKER_ERROR,
    SYSTEM_ERROR
}
