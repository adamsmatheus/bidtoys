package com.leilao.backend.users.application

import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.api.dto.UpdateAddressRequest
import com.leilao.backend.users.domain.User
import com.leilao.backend.users.domain.UserAddress
import com.leilao.backend.users.infrastructure.UserAddressRepository
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateAddressUseCase(
    private val userRepository: UserRepository,
    private val userAddressRepository: UserAddressRepository
) {

    @Transactional
    fun execute(userId: UUID, request: UpdateAddressRequest): User {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val existing = userAddressRepository.findByUserId(userId)
        if (existing.isPresent) {
            val address = existing.get()
            address.cep = request.cep
            address.street = request.street
            address.city = request.city
            address.state = request.state
            address.number = request.number
            address.complement = request.complement
            userAddressRepository.save(address)
        } else {
            userAddressRepository.save(
                UserAddress(
                    user = user,
                    cep = request.cep,
                    street = request.street,
                    city = request.city,
                    state = request.state,
                    number = request.number,
                    complement = request.complement
                )
            )
        }

        return userRepository.findById(userId).get()
    }
}
