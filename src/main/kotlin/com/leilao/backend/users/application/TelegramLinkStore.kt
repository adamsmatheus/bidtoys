package com.leilao.backend.users.application

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class TelegramLinkStore {

    private data class Entry(
        val userId: UUID,
        val expiresAt: Instant,
        val chatId: Long? = null,
        val linked: Boolean = false
    )

    private val store = ConcurrentHashMap<String, Entry>()

    fun create(userId: UUID, ttlSeconds: Long = 600): String {
        val token = UUID.randomUUID().toString()
        store[token] = Entry(userId, Instant.now().plusSeconds(ttlSeconds))
        return token
    }

    fun markLinked(token: String, chatId: Long): UUID? {
        val entry = store[token] ?: return null
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return null
        }
        store[token] = entry.copy(chatId = chatId, linked = true)
        return entry.userId
    }

    fun isLinked(token: String): Boolean {
        val entry = store[token] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(token)
            return false
        }
        return entry.linked
    }

    fun remove(token: String) {
        store.remove(token)
    }
}
