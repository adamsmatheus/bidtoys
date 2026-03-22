package com.leilao.backend.users.api

import com.leilao.backend.shared.security.UserPrincipal
import com.leilao.backend.users.api.dto.CreateUserRequest
import com.leilao.backend.users.api.dto.UpdateUserRequest
import com.leilao.backend.users.api.dto.UserResponse
import com.leilao.backend.users.application.CreateUserUseCase
import com.leilao.backend.users.application.DeleteUserUseCase
import com.leilao.backend.users.application.GetUserUseCase
import com.leilao.backend.users.application.UpdateUserUseCase
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
    private val getUserUseCase: GetUserUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil do usuário autenticado")
    fun me(@AuthenticationPrincipal principal: UserPrincipal): UserResponse =
        UserResponse.from(getUserUseCase.execute(principal.id))

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os usuários")
    fun listAll(): List<UserResponse> = getUserUseCase.executeAll().map { UserResponse.from(it) }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retorna um usuário pelo ID")
    fun findById(@PathVariable id: UUID): UserResponse = UserResponse.from(getUserUseCase.execute(id))

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo usuário")
    fun create(@Valid @RequestBody request: CreateUserRequest): UserResponse =
        UserResponse.from(createUserUseCase.execute(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza os dados de um usuário")
    fun update(
        @PathVariable id: UUID, @Valid @RequestBody request: UpdateUserRequest
    ): UserResponse = UserResponse.from(updateUserUseCase.execute(id, request))

    @PatchMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promove um usuário para administrador")
    fun promote(@PathVariable id: UUID): UserResponse = UserResponse.from(updateUserUseCase.promoteToAdmin(id))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um usuário")
    fun delete(@PathVariable id: UUID) = deleteUserUseCase.execute(id)
}
