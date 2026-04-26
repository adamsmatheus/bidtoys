package com.leilao.backend.rifas.domain

enum class RifaStatus {
    DRAFT,
    PENDING_APPROVAL,
    REJECTED,
    READY_TO_START,
    ACTIVE,
    FINISHED,
    CANCELLED;

    fun canEdit() = this == DRAFT || this == REJECTED
    fun canSubmitForApproval() = this == DRAFT || this == REJECTED
    fun canApproveOrReject() = this == PENDING_APPROVAL
    fun canStart() = this == READY_TO_START
    fun canCancel() = this == DRAFT || this == REJECTED || this == READY_TO_START
    fun isActive() = this == ACTIVE
}
