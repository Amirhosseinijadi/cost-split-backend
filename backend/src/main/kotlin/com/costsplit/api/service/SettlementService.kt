package com.costsplit.api.service

import com.costsplit.api.api.CreateSettlementRequest
import com.costsplit.api.api.SettlementResponse
import com.costsplit.api.domain.ExpenseGroupRepository
import com.costsplit.api.domain.GroupMemberRepository
import com.costsplit.api.domain.SettlementEntity
import com.costsplit.api.domain.SettlementRepository
import com.costsplit.api.domain.UserRepository
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class SettlementService(
    private val groupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val settlementRepository: SettlementRepository,
    private val database: CoroutineDatabaseExecutor,
) {
    suspend fun create(groupId: UUID, request: CreateSettlementRequest): SettlementResponse = database.write {
        if (request.fromUserId == request.toUserId) {
            throw InvalidRequestException("Settlement users must be different")
        }

        val group = groupRepository.findById(groupId)
            .orElseThrow { NotFoundException("Group $groupId was not found") }
        val usersById = userRepository.findAllById(setOf(request.fromUserId, request.toUserId)).associateBy { it.id }
        val missingUserIds = setOf(request.fromUserId, request.toUserId) - usersById.keys
        if (missingUserIds.isNotEmpty()) {
            throw NotFoundException("Users not found: ${missingUserIds.sorted().joinToString()}")
        }

        val nonMemberIds = usersById.keys.filterNot { groupMemberRepository.existsByGroupIdAndUserId(groupId, it) }
        if (nonMemberIds.isNotEmpty()) {
            throw InvalidRequestException("Users are not members of group $groupId: ${nonMemberIds.sorted().joinToString()}")
        }

        val settlement = settlementRepository.save(
            SettlementEntity(
                group = group,
                fromUser = usersById.getValue(request.fromUserId),
                toUser = usersById.getValue(request.toUserId),
                amount = request.amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY),
                currency = request.currency.uppercase(),
                note = request.note?.trim()?.takeIf { it.isNotEmpty() },
                settledOn = request.settledOn ?: LocalDate.now(),
            ),
        )
        settlement.toResponse()
    }

    suspend fun listForGroup(groupId: UUID): List<SettlementResponse> = database.read {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("Group $groupId was not found")
        }
        settlementRepository.findAllForGroup(groupId).map { it.toResponse() }
    }

    private fun SettlementEntity.toResponse() = SettlementResponse(
        id = id,
        groupId = group.id,
        fromUserId = fromUser.id,
        fromDisplayName = fromUser.displayName,
        toUserId = toUser.id,
        toDisplayName = toUser.displayName,
        amount = amount,
        currency = currency,
        note = note,
        settledOn = settledOn,
        createdAt = createdAt,
    )

    private companion object {
        const val MONEY_SCALE = 2
    }
}
