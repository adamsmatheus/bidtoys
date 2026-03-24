package com.leilao.backend.auth.application

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class WhatsAppVerificationStore {

    private data class Entry(val code: String, val expiresAt: Instant)

    private val store = ConcurrentHashMap<String, Entry>()

    fun save(phoneNumber: String, code: String, ttlSeconds: Long = 600) {
        store[normalize(phoneNumber)] = Entry(code, Instant.now().plusSeconds(ttlSeconds))
    }

    fun verify(phoneNumber: String, code: String): Boolean {
        val entry = store[normalize(phoneNumber)] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(normalize(phoneNumber))
            return false
        }
        return entry.code == code
    }

    fun remove(phoneNumber: String) {
        store.remove(normalize(phoneNumber))
    }

    private fun normalize(phoneNumber: String) = phoneNumber.replace(Regex("[^0-9+]"), "")
}
