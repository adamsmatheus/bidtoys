package com.leilao.backend.auth.application

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class TelegramVerificationStore {

    private data class Entry(
        val phoneNumber: String,
        val chatId: Long?,
        val expiresAt: Instant,
        val verified: Boolean = false
    )

    private val store = ConcurrentHashMap<String, Entry>()

    fun create(phoneNumber: String, ttlSeconds: Long = 600): String {
        val token = UUID.randomUUID().toString()
        store[token] = Entry(phoneNumber, null, Instant.now().plusSeconds(ttlSeconds))
        return token
    }

    fun markVerified(token: String, chatId: Long): Boolean {
        val entry = store[token] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return false
        }
        store[token] = entry.copy(chatId = chatId, verified = true)
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

    fun getIfVerified(token: String): Pair<String, Long>? {
        val entry = store[token] ?: return null
        if (!entry.verified || entry.chatId == null) return null
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return null
        }
        return Pair(entry.phoneNumber, entry.chatId)
    }

    fun remove(token: String) {
        store.remove(token)
    }
}
