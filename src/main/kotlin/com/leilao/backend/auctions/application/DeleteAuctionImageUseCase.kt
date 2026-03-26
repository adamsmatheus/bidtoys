package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.infrastructure.AuctionImageRepository
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.storage.StorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteAuctionImageUseCase(
    private val auctionImageRepository: AuctionImageRepository,
    private val storageService: StorageService
) {

    @Transactional
    fun execute(auctionId: UUID, imageId: UUID, userId: UUID) {
        val image = auctionImageRepository.findById(imageId)
            .orElseThrow { NotFoundException("Imagem não encontrada") }

        if (image.auction.id != auctionId) {
            throw NotFoundException("Imagem não encontrada")
        }

        if (!image.auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para remover esta imagem")
        }

        if (!image.auction.status.canEdit()) {
            throw InvalidStateException(
                "Não é possível remover imagens de um leilão no estado ${image.auction.status}",
                "INVALID_STATE_FOR_IMAGE_DELETE"
            )
        }

        storageService.delete(image.fileKey)
        auctionImageRepository.delete(image)
    }
}
