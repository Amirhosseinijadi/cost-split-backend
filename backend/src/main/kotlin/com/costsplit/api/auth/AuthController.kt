package com.costsplit.api.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    suspend fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @GetMapping("/validate")
    fun validate(@AuthenticationPrincipal principal: UserPrincipal, authentication: Authentication): TokenValidationResponse {
        val token = authentication.credentials.toString()
        val claims = jwtService.validate(token)
        return TokenValidationResponse(
            valid = true,
            userId = principal.id,
            email = principal.email,
            expiresAt = claims.expiresAt,
        )
    }
}
