package com.leilao.backend.shared.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long
) {

    private val log = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(userId: UUID, email: String, role: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)
            true
        } catch (ex: JwtException) {
            log.debug("JWT inválido: {}", ex.message)
            false
        } catch (ex: IllegalArgumentException) {
            log.debug("JWT token claim vazio: {}", ex.message)
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = Jwts.parser().verifyWith(signingKey).build()
            .parseSignedClaims(token).payload
        return UUID.fromString(claims.subject)
    }

    fun getEmailFromToken(token: String): String {
        val claims = Jwts.parser().verifyWith(signingKey).build()
            .parseSignedClaims(token).payload
        return claims["email"] as String
    }

    fun getRoleFromToken(token: String): String {
        val claims = Jwts.parser().verifyWith(signingKey).build()
            .parseSignedClaims(token).payload
        return claims["role"] as String
    }
}
