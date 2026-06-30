package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.storage.StorageService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp", "application/pdf")
private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L

@Service
class UploadPaymentReceiptUseCase(
    private val auctionRepository: AuctionRepository,
    private val storageService: StorageService
) {

    fun execute(auctionId: UUID, userId: UUID, file: MultipartFile): String {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (auction.winnerUserId != userId) {
            throw ForbiddenException("Somente o vencedor pode enviar o comprovante de pagamento")
        }

        if (auction.status != AuctionStatus.FINISHED_WITH_WINNER) {
            throw InvalidStateException(
                "Comprovante só pode ser enviado antes de declarar o pagamento",
                "INVALID_STATE_FOR_RECEIPT_UPLOAD"
            )
        }

        val contentType = file.contentType ?: ""
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException(
                "Formato não suportado. Use JPEG, PNG, WEBP ou PDF",
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

        val ext = when (contentType) {
            "application/pdf" -> "pdf"
            else -> contentType.substringAfter("/").replace("jpeg", "jpg")
        }
        val fileKey = "auctions/$auctionId/receipt/${UUID.randomUUID()}.$ext"
        storageService.store(file, fileKey)

        return storageService.toUrl(fileKey)
    }
}
