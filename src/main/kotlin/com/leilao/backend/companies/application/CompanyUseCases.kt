package com.leilao.backend.companies.application

import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.companies.api.dto.UpsertCompanyRequest
import com.leilao.backend.companies.domain.Company
import com.leilao.backend.companies.infrastructure.CompanyRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.storage.StorageService
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private val DELETABLE_STATUSES = setOf(AuctionStatus.DRAFT, AuctionStatus.REJECTED, AuctionStatus.CANCELLED)

private val ALLOWED_LOGO_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024L

@Service
class GetMyCompanyUseCase(
    private val companyRepository: CompanyRepository
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID): Company? {
        return companyRepository.findByUserId(userId).orElse(null)
    }
}

@Service
class UpsertCompanyUseCase(
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun execute(request: UpsertCompanyRequest, userId: UUID): Company {
        val existing = companyRepository.findByUserId(userId).orElse(null)

        return if (existing != null) {
            existing.name = request.name
            existing.description = request.description
            existing.logoUrl = request.logoUrl
            existing.pixKey = request.pixKey
            companyRepository.save(existing)
        } else {
            if (request.pixKey.isNullOrBlank()) {
                throw BusinessException(
                    "Chave PIX é obrigatória para o cadastro da empresa",
                    "PIX_KEY_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY
                )
            }
            val user = userRepository.findById(userId)
                .orElseThrow { NotFoundException("Usuário não encontrado") }
            companyRepository.save(
                Company(user = user, name = request.name, description = request.description, logoUrl = request.logoUrl, pixKey = request.pixKey)
            )
        }
    }
}

@Service
class UploadCompanyLogoUseCase(
    private val storageService: StorageService
) {
    fun execute(userId: UUID, file: MultipartFile): String {
        val contentType = file.contentType ?: ""
        if (contentType !in ALLOWED_LOGO_CONTENT_TYPES) {
            throw BusinessException(
                "Formato não suportado. Use JPEG, PNG ou WEBP",
                "INVALID_FILE_TYPE",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }
        if (file.size > MAX_LOGO_SIZE_BYTES) {
            throw BusinessException(
                "Arquivo muito grande. Máximo 5 MB",
                "FILE_TOO_LARGE",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }
        val ext = contentType.substringAfter("/").replace("jpeg", "jpg")
        val fileKey = "companies/$userId/${UUID.randomUUID()}.$ext"
        storageService.store(file, fileKey)
        return storageService.toUrl(fileKey)
    }
}

@Service
class DeleteMyCompanyUseCase(
    private val companyRepository: CompanyRepository,
    private val auctionRepository: AuctionRepository
) {
    @Transactional
    fun execute(userId: UUID) {
        val company = companyRepository.findByUserId(userId)
            .orElseThrow { NotFoundException("Empresa não encontrada") }

        if (auctionRepository.existsBySellerIdAndStatusNotIn(userId, DELETABLE_STATUSES)) {
            throw BusinessException(
                "Não é possível excluir a empresa com leilões em andamento",
                "COMPANY_HAS_ACTIVE_AUCTIONS",
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

        companyRepository.delete(company)
    }
}

@Service
class ListActiveCompaniesUseCase(
    private val companyRepository: CompanyRepository
) {
    @Transactional(readOnly = true)
    fun execute(): List<Company> {
        return companyRepository.findCompaniesWithActiveAuctions()
    }
}

@Service
class ListAllCompaniesUseCase(
    private val companyRepository: CompanyRepository
) {
    @Transactional(readOnly = true)
    fun execute(): List<Company> {
        return companyRepository.findAll().sortedBy { it.name }
    }
}
