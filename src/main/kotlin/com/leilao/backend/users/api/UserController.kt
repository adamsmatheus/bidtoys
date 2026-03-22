package com.leilao.backend.users.api

import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.security.UserPrincipal
import com.leilao.backend.users.api.dto.CreateUserRequest
import com.leilao.backend.users.api.dto.UpdateUserRequest
import com.leilao.backend.users.api.dto.UserResponse
import com.leilao.backend.users.application.CreateUserUseCase
import com.leilao.backend.users.application.DeleteUserUseCase
import com.leilao.backend.users.application.UpdateUserUseCase
import com.leilao.backend.users.domain.UserRole
import com.leilao.backend.users.infrastructure.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gerenciamento de usuários")
class UserController(
    private val userRepository: UserRepository,
    private val createUserUseCase: CreateUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil do usuário autenticado")
    fun me(@AuthenticationPrincipal principal: UserPrincipal): UserResponse {
        val user = userRepository.findById(principal.id)
            .orElseThrow { NotFoundException("Usuário não encontrado") }
        return UserResponse.from(user)
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os usuários")
    fun listAll(): List<UserResponse> =
        userRepository.findAll().map { UserResponse.from(it) }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retorna um usuário pelo ID")
    fun findById(@PathVariable id: UUID): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuário não encontrado") }
        return UserResponse.from(user)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo usuário")
    fun create(@Valid @RequestBody request: CreateUserRequest): UserResponse {
        val user = createUserUseCase.execute(request)
        return UserResponse.from(user)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza os dados de um usuário")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest
    ): UserResponse {
        val user = updateUserUseCase.execute(id, request)
        return UserResponse.from(user)
    }

    @PatchMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promove um usuário para administrador")
    fun promote(@PathVariable id: UUID): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuário não encontrado") }
        user.role = UserRole.ADMIN
        return UserResponse.from(userRepository.save(user))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um usuário")
    fun delete(@PathVariable id: UUID) {
        deleteUserUseCase.execute(id)
    }
}
