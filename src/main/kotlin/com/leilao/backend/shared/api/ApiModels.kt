package com.leilao.backend.shared.api

import org.springframework.data.domain.Page

data class ErrorResponse(
    val code: String,
    val message: String,
    val fields: List<FieldError>? = null
)

data class FieldError(
    val field: String,
    val message: String
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> = PageResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast
        )
    }
}
