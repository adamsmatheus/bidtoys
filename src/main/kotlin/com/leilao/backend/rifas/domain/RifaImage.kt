package com.leilao.backend.rifas.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "rifa_images")
class RifaImage(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rifa_id", nullable = false)
    val rifa: Rifa,

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    val fileUrl: String,

    @Column(name = "position", nullable = false)
    val position: Int = 0,

) : BaseEntity()
