package com.leilao.backend.auctions.domain

enum class AuctionStatus {
    DRAFT,
    PENDING_APPROVAL,
    REJECTED,
    READY_TO_START,
    CANCELLED,
    ACTIVE,
    FINISHED_WITH_WINNER,
    FINISHED_NO_BIDS,
    PAYMENT_DECLARED,
    PAYMENT_CONFIRMED,
    PAYMENT_DISPUTED;

    fun canEdit() = this == DRAFT || this == REJECTED

    fun canSubmitForApproval() = this == DRAFT || this == REJECTED

    fun canApproveOrReject() = this == PENDING_APPROVAL

    fun canStart() = this == READY_TO_START

    fun canCancel() = this == DRAFT || this == REJECTED || this == READY_TO_START

    fun isActive() = this == ACTIVE

    fun isTerminal() = this == CANCELLED || this == FINISHED_WITH_WINNER || this == FINISHED_NO_BIDS
            || this == PAYMENT_DECLARED || this == PAYMENT_CONFIRMED || this == PAYMENT_DISPUTED
}
