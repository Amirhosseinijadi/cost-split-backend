package com.costsplit.api.auth

import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    private val objectMapper: ObjectMapper,
    @Value("\${app.auth.jwt-secret}") private val secret: String,
    @Value("\${app.auth.token-ttl}") tokenTtl: String,
) {
    private val tokenLifetime: Duration = parseDuration(tokenTtl)
    private val clock: Clock = Clock.systemUTC()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun createToken(user: AuthUserResponse): SignedToken {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(tokenLifetime)
        val header = mapOf("alg" to ALGORITHM, "typ" to "JWT")
        val payload = mapOf(
            "sub" to user.id.toString(),
            "email" to user.email,
            "iat" to issuedAt.epochSecond,
            "exp" to expiresAt.epochSecond,
        )
        val unsignedToken = "${encodeJson(header)}.${encodeJson(payload)}"
        return SignedToken(
            value = "$unsignedToken.${sign(unsignedToken)}",
            expiresAt = expiresAt,
        )
    }

    fun validate(token: String): TokenClaims {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw InvalidTokenException("Token must have three parts")
        }

        val unsignedToken = "${parts[0]}.${parts[1]}"
        val expectedSignature = sign(unsignedToken)
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw InvalidTokenException("Token signature is invalid")
        }

        val header = decodeJson(parts[0])
        if (header["alg"] != ALGORITHM || header["typ"] != "JWT") {
            throw InvalidTokenException("Token header is invalid")
        }

        val payload = decodeJson(parts[1])
        val expiresAt = Instant.ofEpochSecond((payload["exp"] as? Number)?.toLong() ?: throw InvalidTokenException("Token expiration is missing"))
        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw ExpiredTokenException("Token has expired")
        }

        val userId = UUID.fromString(payload["sub"] as? String ?: throw InvalidTokenException("Token subject is missing"))
        val email = payload["email"] as? String ?: throw InvalidTokenException("Token email is missing")
        return TokenClaims(userId = userId, email = email, expiresAt = expiresAt)
    }

    private fun encodeJson(value: Any): String = encoder.encodeToString(objectMapper.writeValueAsBytes(value))

    private fun decodeJson(value: String): Map<String, Any> =
        objectMapper.readValue(decoder.decode(value), Map::class.java)
            .mapKeys { it.key.toString() }
            .mapValues { it.value as Any }

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return encoder.encodeToString(mac.doFinal(value.toByteArray(Charsets.UTF_8)))
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val leftBytes = left.toByteArray(Charsets.UTF_8)
        val rightBytes = right.toByteArray(Charsets.UTF_8)
        if (leftBytes.size != rightBytes.size) return false
        var result = 0
        for (index in leftBytes.indices) {
            result = result or (leftBytes[index].toInt() xor rightBytes[index].toInt())
        }
        return result == 0
    }

    private fun parseDuration(value: String): Duration {
        val normalized = value.trim().lowercase()
        return when {
            normalized.endsWith("d") -> Duration.ofDays(normalized.dropLast(1).toLong())
            normalized.endsWith("h") -> Duration.ofHours(normalized.dropLast(1).toLong())
            normalized.endsWith("m") -> Duration.ofMinutes(normalized.dropLast(1).toLong())
            normalized.endsWith("s") -> Duration.ofSeconds(normalized.dropLast(1).toLong())
            else -> Duration.parse(value)
        }
    }

    companion object {
        private const val ALGORITHM = "HS256"
    }
}

data class SignedToken(
    val value: String,
    val expiresAt: Instant,
)

data class TokenClaims(
    val userId: UUID,
    val email: String,
    val expiresAt: Instant,
)

open class InvalidTokenException(message: String) : RuntimeException(message)
class ExpiredTokenException(message: String) : InvalidTokenException(message)
