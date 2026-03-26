package com.leilao.backend.users.domain

import com.leilao.backend.shared.domain.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_addresses")
class UserAddress(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "cep", nullable = false, length = 9)
    var cep: String,

    @Column(name = "street", nullable = false, length = 255)
    var street: String,

    @Column(name = "city", nullable = false, length = 100)
    var city: String,

    @Column(name = "state", nullable = false, length = 2)
    var state: String,

    @Column(name = "number", nullable = false, length = 20)
    var number: String,

    @Column(name = "complement", length = 255)
    var complement: String? = null

) : AuditableEntity()
