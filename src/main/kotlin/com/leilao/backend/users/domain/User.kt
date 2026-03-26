package com.leilao.backend.users.domain

import com.leilao.backend.shared.domain.AuditableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(

    @Column(name = "name", nullable = false, length = 150)
    var name: String,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "phone_number", nullable = false, length = 20)
    var phoneNumber: String,

    @Column(name = "whatsapp_enabled", nullable = false)
    var whatsappEnabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
    var address: UserAddress? = null

) : AuditableEntity() {

    fun isAdmin() = role == UserRole.ADMIN

    fun isActive() = status == UserStatus.ACTIVE

    fun block() {
        status = UserStatus.BLOCKED
    }

    fun activate() {
        status = UserStatus.ACTIVE
    }
}
