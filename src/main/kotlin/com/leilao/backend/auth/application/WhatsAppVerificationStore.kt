package com.leilao.backend.auth.application

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class WhatsAppVerificationStore {

    private data class Entry(
        val phoneNumber: String,
        val code: String,
        val expiresAt: Instant,
        val verified: Boolean = false
    )

    private val store = ConcurrentHashMap<String, Entry>()

    fun create(phoneNumber: String, code: String, ttlSeconds: Long = 600): String {
        val token = UUID.randomUUID().toString()
        store[token] = Entry(phoneNumber, code, Instant.now().plusSeconds(ttlSeconds))
        return token
    }

    fun verify(token: String, code: String): Boolean {
        val entry = store[token] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return false
        }
        if (entry.code != code) return false
        store[token] = entry.copy(verified = true)
        return true
    }

    fun isVerified(token: String): Boolean {
        val entry = store[token] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return false
        }
        return entry.verified
    }

    fun getIfVerified(token: String): String? {
        val entry = store[token] ?: return null
        if (!entry.verified) return null
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return null
        }
        return entry.phoneNumber
    }

    fun remove(token: String) {
        store.remove(token)
    }
}
