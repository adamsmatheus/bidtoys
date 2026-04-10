package com.leilao.backend.auth.application

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class PasswordResetStore {

    private data class Entry(val code: String, val expiresAt: Instant)

    private val store = ConcurrentHashMap<String, Entry>()

    fun save(email: String, code: String, ttlSeconds: Long = 900) {
        store[email.lowercase().trim()] = Entry(code, Instant.now().plusSeconds(ttlSeconds))
    }

    fun verify(email: String, code: String): Boolean {
        val key = email.lowercase().trim()
        val entry = store[key] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(key)
            return false
        }
        return entry.code == code
    }

    fun remove(email: String) {
        store.remove(email.lowercase().trim())
    }
}
