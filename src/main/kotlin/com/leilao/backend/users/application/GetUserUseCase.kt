package com.leilao.backend.users.application

import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetUserUseCase(
    private val userRepository: UserRepository
) {

    fun execute(id: UUID): User =
        userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

    fun executeAll(): List<User> =
        userRepository.findAll()
}
