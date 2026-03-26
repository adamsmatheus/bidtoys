package com.leilao.backend.shared.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

interface StorageService {
    fun store(file: MultipartFile, fileKey: String)
    fun delete(fileKey: String)
    fun toUrl(fileKey: String): String
}

@Service
class LocalStorageService(
    @Value("\${app.storage.upload-dir}") private val uploadDir: String,
    @Value("\${app.storage.base-url}") private val baseUrl: String
) : StorageService {

    override fun store(file: MultipartFile, fileKey: String) {
        val target = Path.of(uploadDir, fileKey)
        Files.createDirectories(target.parent)
        Files.copy(file.inputStream, target, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun delete(fileKey: String) {
        Files.deleteIfExists(Path.of(uploadDir, fileKey))
    }

    override fun toUrl(fileKey: String): String = "$baseUrl/files/$fileKey"
}
