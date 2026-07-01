package com.leilao.backend.reports.application

import com.leilao.backend.reports.api.dto.BuyerReportResponse
import com.leilao.backend.reports.domain.BuyerReport
import com.leilao.backend.reports.infrastructure.BuyerReportRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.storage.StorageService
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L
private const val MAX_IMAGES = 3

@Service
class SubmitBuyerReportUseCase(
    private val buyerReportRepository: BuyerReportRepository,
    private val userRepository: UserRepository,
    private val storageService: StorageService
) {

    @Transactional
    fun execute(
        reporterId: UUID,
        reportedUserId: UUID,
        reason: String,
        images: List<MultipartFile>
    ): UUID {
        val reporter = userRepository.findById(reporterId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val reportedUser = userRepository.findById(reportedUserId)
            .orElseThrow { NotFoundException("Comprador não encontrado") }

        if (images.size > MAX_IMAGES) {
            throw BusinessException(
                "Máximo de $MAX_IMAGES imagens permitidas",
                "MAX_IMAGES_EXCEEDED",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

        val imageUrls = images.mapIndexed { index, file ->
            val contentType = file.contentType ?: ""
            if (contentType !in ALLOWED_CONTENT_TYPES) {
                throw BusinessException(
                    "Formato não suportado. Use JPEG, PNG ou WEBP",
                    "INVALID_FILE_TYPE",
                    HttpStatus.UNPROCESSABLE_ENTITY
                )
            }
            if (file.size > MAX_FILE_SIZE_BYTES) {
                throw BusinessException(
                    "Arquivo muito grande. Máximo 5 MB",
                    "FILE_TOO_LARGE",
                    HttpStatus.UNPROCESSABLE_ENTITY
                )
            }
            val ext = contentType.substringAfter("/").replace("jpeg", "jpg")
            val fileKey = "reports/${UUID.randomUUID()}.$ext"
            storageService.store(file, fileKey)
            storageService.toUrl(fileKey)
        }

        val report = buyerReportRepository.save(
            BuyerReport(
                reporterId = reporterId,
                reporterName = reporter.name,
                reportedUserId = reportedUserId,
                reportedUserName = reportedUser.name,
                reason = reason,
                imageUrls = imageUrls.toMutableList()
            )
        )

        return report.id
    }
}
