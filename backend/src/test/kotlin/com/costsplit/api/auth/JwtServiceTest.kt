package com.costsplit.api.auth

import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtServiceTest {
    private val jwtService = JwtService(
        objectMapper = ObjectMapper(),
        secret = "test-secret-that-is-long-enough-for-hmac-signing",
        tokenTtl = "30d",
    )

    @Test
    fun `creates a token that validates with a 30 day expiry`() {
        val user = AuthUserResponse(
            id = UUID.randomUUID(),
            displayName = "Amir",
            email = "amir@example.com",
        )

        val signedToken = jwtService.createToken(user)
        val claims = jwtService.validate(signedToken.value)

        assertEquals(user.id, claims.userId)
        assertEquals(user.email, claims.email)
        val remainingLifetime = Duration.between(Instant.now(), claims.expiresAt)
        assertTrue(remainingLifetime.toDays() in 29..30)
    }

    @Test
    fun `rejects a tampered token`() {
        val user = AuthUserResponse(
            id = UUID.randomUUID(),
            displayName = "Amir",
            email = "amir@example.com",
        )
        val signedToken = jwtService.createToken(user)
        val tamperedToken = signedToken.value.dropLast(2) + "xx"

        assertThrows<InvalidTokenException> {
            jwtService.validate(tamperedToken)
        }
    }
}
