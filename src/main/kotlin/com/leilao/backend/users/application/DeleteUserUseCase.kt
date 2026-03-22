package com.leilao.backend.users.application

import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteUserUseCase(
    private val userRepository: UserRepository
) {

    @Transactional
    fun execute(id: UUID) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("Usuário não encontrado")
        }
        userRepository.deleteById(id)
    }
}
