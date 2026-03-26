package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.api.dto.AuctionImageResponse
import com.leilao.backend.auctions.domain.AuctionImage
import com.leilao.backend.auctions.infrastructure.AuctionImageRepository
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.storage.StorageService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L
private const val MAX_IMAGES_PER_AUCTION = 5

@Service
class UploadAuctionImageUseCase(
    private val auctionRepository: AuctionRepository,
    private val auctionImageRepository: AuctionImageRepository,
    private val storageService: StorageService
) {

    @Transactional
    fun execute(auctionId: UUID, userId: UUID, file: MultipartFile): AuctionImageResponse {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para adicionar imagens a este leilão")
        }

        if (!auction.status.canEdit()) {
            throw InvalidStateException(
                "Não é possível adicionar imagens a um leilão no estado ${auction.status}",
                "INVALID_STATE_FOR_IMAGE_UPLOAD"
            )
        }

        val existingImages = auctionImageRepository.findByAuction_IdOrderByPositionAsc(auctionId)
        if (existingImages.size >= MAX_IMAGES_PER_AUCTION) {
            throw BusinessException(
                "Limite máximo de $MAX_IMAGES_PER_AUCTION fotos atingido",
                "MAX_IMAGES_REACHED",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

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
        val fileKey = "auctions/$auctionId/${UUID.randomUUID()}.$ext"
        storageService.store(file, fileKey)

        val nextPosition = if (existingImages.isEmpty()) 0 else existingImages.last().position + 1
        val image = auctionImageRepository.save(
            AuctionImage(
                auction = auction,
                fileKey = fileKey,
                fileUrl = storageService.toUrl(fileKey),
                position = nextPosition
            )
        )

        return AuctionImageResponse(id = image.id, fileUrl = image.fileUrl, position = image.position)
    }
}
