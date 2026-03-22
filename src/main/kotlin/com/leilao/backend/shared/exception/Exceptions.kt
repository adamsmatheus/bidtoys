package com.leilao.backend.shared.exception

import org.springframework.http.HttpStatus

open class BusinessException(
    message: String,
    val code: String,
    val status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY
) : RuntimeException(message)

class NotFoundException(
    message: String,
    code: String = "NOT_FOUND"
) : BusinessException(message, code, HttpStatus.NOT_FOUND)

class ForbiddenException(
    message: String,
    code: String = "FORBIDDEN"
) : BusinessException(message, code, HttpStatus.FORBIDDEN)

class ConflictException(
    message: String,
    code: String = "CONFLICT"
) : BusinessException(message, code, HttpStatus.CONFLICT)

class InvalidStateException(
    message: String,
    code: String = "INVALID_STATE"
) : BusinessException(message, code, HttpStatus.UNPROCESSABLE_ENTITY)
