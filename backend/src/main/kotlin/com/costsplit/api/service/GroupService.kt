package com.costsplit.api.service

import com.costsplit.api.api.AddGroupMemberRequest
import com.costsplit.api.api.CreateGroupRequest
import com.costsplit.api.api.GroupMemberResponse
import com.costsplit.api.api.GroupResponse
import com.costsplit.api.domain.ExpenseGroupEntity
import com.costsplit.api.domain.ExpenseGroupRepository
import com.costsplit.api.domain.GroupMemberEntity
import com.costsplit.api.domain.GroupMemberRepository
import com.costsplit.api.domain.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val database: CoroutineDatabaseExecutor,
) {
    suspend fun create(request: CreateGroupRequest): GroupResponse = database.write {
        val allUserIds = request.memberUserIds + request.ownerUserId
        val usersById = userRepository.findAllById(allUserIds).associateBy { it.id }
        val missingUserIds = allUserIds - usersById.keys
        if (missingUserIds.isNotEmpty()) {
            throw NotFoundException("Users not found: ${missingUserIds.sorted().joinToString()}")
        }

        val group = groupRepository.save(
            ExpenseGroupEntity(
                name = request.name.trim(),
                owner = usersById.getValue(request.ownerUserId),
                icon = request.icon?.trim()?.takeIf { it.isNotEmpty() },
                color = request.color?.uppercase(),
            ),
        )
        val members = groupMemberRepository.saveAll(
            allUserIds.map { userId -> GroupMemberEntity(group = group, user = usersById.getValue(userId)) },
        )
        group.toResponse(members)
    }

    suspend fun addMember(groupId: UUID, request: AddGroupMemberRequest): GroupResponse = database.write {
        val group = findEntity(groupId)
        val user = userRepository.findById(request.userId)
            .orElseThrow { NotFoundException("User ${request.userId} was not found") }
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, request.userId)) {
            throw ConflictException("User ${request.userId} is already a member of group $groupId")
        }
        groupMemberRepository.save(GroupMemberEntity(group = group, user = user))
        group.toResponse(groupMemberRepository.findMembersWithUsers(groupId))
    }

    suspend fun get(groupId: UUID): GroupResponse = database.read {
        val group = findEntity(groupId)
        group.toResponse(groupMemberRepository.findMembersWithUsers(groupId))
    }

    suspend fun listForUser(userId: UUID): List<GroupResponse> = database.read {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("User $userId was not found")
        }
        groupRepository.findAllForUser(userId).map { group ->
            group.toResponse(groupMemberRepository.findMembersWithUsers(group.id))
        }
    }

    private fun findEntity(groupId: UUID): ExpenseGroupEntity = groupRepository.findById(groupId)
        .orElseThrow { NotFoundException("Group $groupId was not found") }
}

private fun ExpenseGroupEntity.toResponse(members: List<GroupMemberEntity>) = GroupResponse(
    id = id,
    name = name,
    ownerUserId = owner.id,
    icon = icon,
    color = color,
    members = members.map {
        GroupMemberResponse(
            userId = it.user.id,
            displayName = it.user.displayName,
            email = it.user.email,
            joinedAt = it.joinedAt,
        )
    },
    createdAt = createdAt,
)
