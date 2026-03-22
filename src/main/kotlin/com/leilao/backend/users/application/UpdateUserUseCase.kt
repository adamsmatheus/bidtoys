package com.leilao.backend.users.application

import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.api.dto.UpdateUserRequest
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.domain.UserStatus
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateUserUseCase(
    private val userRepository: UserRepository
) {

    @Transactional
    fun execute(id: UUID, request: UpdateUserRequest): User {
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        request.name?.let { user.name = it }
        request.phoneNumber?.let { user.phoneNumber = it }
        request.whatsappEnabled?.let { user.whatsappEnabled = it }
        request.role?.let { user.role = it }
        request.status?.let {
            when (it) {
                UserStatus.ACTIVE -> user.activate()
                UserStatus.BLOCKED -> user.block()
            }
        }

        return userRepository.save(user)
    }

    @Transactional
    fun promoteToAdmin(id: UUID): User {
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuário não encontrado") }
        user.role = UserRole.ADMIN
        return userRepository.save(user)
    }
}
