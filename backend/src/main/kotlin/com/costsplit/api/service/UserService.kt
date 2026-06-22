package com.costsplit.api.service

import com.costsplit.api.api.CreateUserRequest
import com.costsplit.api.api.UserResponse
import com.costsplit.api.domain.UserEntity
import com.costsplit.api.domain.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val database: CoroutineDatabaseExecutor,
) {
    suspend fun create(request: CreateUserRequest): UserResponse = database.write {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ConflictException("A user with email $normalizedEmail already exists")
        }

        userRepository.save(
            UserEntity(
                displayName = request.displayName.trim(),
                email = normalizedEmail,
            ),
        ).toResponse()
    }

    suspend fun get(id: UUID): UserResponse = database.read {
        findEntity(id).toResponse()
    }

    suspend fun list(): List<UserResponse> = database.read {
        userRepository.findAllByOrderByDisplayNameAsc().map(UserEntity::toResponse)
    }

    private fun findEntity(id: UUID): UserEntity = userRepository.findById(id)
        .orElseThrow { NotFoundException("User $id was not found") }
}

private fun UserEntity.toResponse() = UserResponse(
    id = id,
    displayName = displayName,
    email = email,
    createdAt = createdAt,
)
