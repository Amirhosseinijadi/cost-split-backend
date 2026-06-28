package com.costsplit.api.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val displayName: String,

    @field:NotBlank
    @field:Email
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    val password: String,
)

data class AuthResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val expiresAt: Instant,
    val user: AuthUserResponse,
)

data class AuthUserResponse(
    val id: UUID,
    val displayName: String,
    val email: String,
)

data class TokenValidationResponse(
    val valid: Boolean,
    val userId: UUID,
    val email: String,
    val expiresAt: Instant,
)

data class UserPrincipal(
    val id: UUID,
    val email: String,
)

