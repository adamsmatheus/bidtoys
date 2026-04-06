package com.leilao.backend.companies.domain

import com.leilao.backend.shared.domain.AuditableEntity
import com.leilao.backend.users.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "companies")
class Company(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "logo_url", length = 1024)
    var logoUrl: String? = null,

    @Column(name = "pix_key", length = 150)
    var pixKey: String? = null

) : AuditableEntity()
