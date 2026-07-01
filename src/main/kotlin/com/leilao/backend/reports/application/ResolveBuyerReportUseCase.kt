package com.leilao.backend.reports.application

import com.leilao.backend.reports.domain.BuyerReportStatus
import com.leilao.backend.reports.infrastructure.BuyerReportRepository
import com.leilao.backend.shared.exception.BusinessException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ResolveBuyerReportUseCase(
    private val buyerReportRepository: BuyerReportRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun execute(reportId: UUID, action: String) {
        val report = buyerReportRepository.findById(reportId)
            .orElseThrow { NotFoundException("Reporte não encontrado") }

        if (report.status != BuyerReportStatus.PENDING) {
            throw BusinessException(
                "Este reporte já foi resolvido",
                "REPORT_ALREADY_RESOLVED",
                HttpStatus.CONFLICT
            )
        }

        if (action == "DEACTIVATE") {
            val user = userRepository.findById(report.reportedUserId)
                .orElseThrow { NotFoundException("Usuário não encontrado") }
            user.block()
            userRepository.save(user)
            report.status = BuyerReportStatus.RESOLVED_DEACTIVATED
        } else if (action == "KEEP_ACTIVE") {
            report.status = BuyerReportStatus.RESOLVED_KEPT_ACTIVE
        } else {
            throw BusinessException(
                "Ação inválida. Use DEACTIVATE ou KEEP_ACTIVE",
                "INVALID_ACTION",
                HttpStatus.BAD_REQUEST
            )
        }

        report.resolvedAt = Instant.now()
        buyerReportRepository.save(report)
    }
}
