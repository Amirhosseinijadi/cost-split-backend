package com.costsplit.api.auth

import com.costsplit.api.domain.UserEntity
import com.costsplit.api.domain.UserRepository
import com.costsplit.api.service.ConflictException
import com.costsplit.api.service.CoroutineDatabaseExecutor
import com.costsplit.api.service.UnauthorizedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val database: CoroutineDatabaseExecutor,
) {
    suspend fun register(request: RegisterRequest): AuthResponse = database.write {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ConflictException("A user with email $normalizedEmail already exists")
        }

        val user = userRepository.save(
            UserEntity(
                displayName = request.displayName.trim(),
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(request.password),
            ),
        )
        user.toAuthResponse()
    }

    suspend fun login(request: LoginRequest): AuthResponse = database.read {
        val normalizedEmail = request.email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw UnauthorizedException("Email or password is incorrect")
        val passwordHash = user.passwordHash
            ?: throw UnauthorizedException("Email or password is incorrect")

        if (!passwordEncoder.matches(request.password, passwordHash)) {
            throw UnauthorizedException("Email or password is incorrect")
        }

        user.toAuthResponse()
    }

    private fun UserEntity.toAuthResponse(): AuthResponse {
        val authUser = AuthUserResponse(
            id = id,
            displayName = displayName,
            email = email,
        )
        val token = jwtService.createToken(authUser)
        return AuthResponse(
            accessToken = token.value,
            expiresAt = token.expiresAt,
            user = authUser,
        )
    }
}
