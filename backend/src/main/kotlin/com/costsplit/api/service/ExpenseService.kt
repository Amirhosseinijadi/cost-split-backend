package com.costsplit.api.service

import com.costsplit.api.api.CreateExpenseRequest
import com.costsplit.api.api.ExpenseResponse
import com.costsplit.api.api.ExpenseShareResponse
import com.costsplit.api.domain.ExpenseEntity
import com.costsplit.api.domain.ExpenseGroupRepository
import com.costsplit.api.domain.ExpenseRepository
import com.costsplit.api.domain.ExpenseShareEntity
import com.costsplit.api.domain.ExpenseShareRepository
import com.costsplit.api.domain.GroupMemberRepository
import com.costsplit.api.domain.UserRepository
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.util.UUID

@Service
class ExpenseService(
    private val groupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseShareRepository: ExpenseShareRepository,
    private val splitCalculator: EqualSplitCalculator,
    private val database: CoroutineDatabaseExecutor,
) {
    suspend fun create(groupId: UUID, request: CreateExpenseRequest): ExpenseResponse = database.write {
        val group = groupRepository.findById(groupId)
            .orElseThrow { NotFoundException("Group $groupId was not found") }
        val involvedUserIds = request.participantUserIds + request.paidByUserId
        val usersById = userRepository.findAllById(involvedUserIds).associateBy { it.id }
        val missingUserIds = involvedUserIds - usersById.keys
        if (missingUserIds.isNotEmpty()) {
            throw NotFoundException("Users not found: ${missingUserIds.sorted().joinToString()}")
        }

        val nonMemberIds = involvedUserIds.filterNot { groupMemberRepository.existsByGroupIdAndUserId(groupId, it) }
        if (nonMemberIds.isNotEmpty()) {
            throw InvalidRequestException("Users are not members of group $groupId: ${nonMemberIds.sorted().joinToString()}")
        }

        val sharesByUserId = splitCalculator.split(request.totalAmount, request.participantUserIds)
        val normalizedTotal = request.totalAmount.setScale(EqualSplitCalculator.MONEY_SCALE, RoundingMode.UNNECESSARY)
        val expense = expenseRepository.save(
            ExpenseEntity(
                group = group,
                description = request.description.trim(),
                totalAmount = normalizedTotal,
                currency = request.currency.uppercase(),
                paidBy = usersById.getValue(request.paidByUserId),
            ),
        )
        val shares = expenseShareRepository.saveAll(
            sharesByUserId.map { (userId, amount) ->
                ExpenseShareEntity(expense = expense, user = usersById.getValue(userId), amountOwed = amount)
            },
        )
        expense.toResponse(shares)
    }

    suspend fun get(expenseId: UUID): ExpenseResponse = database.read {
        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { NotFoundException("Expense $expenseId was not found") }
        expense.toResponse(expenseShareRepository.findAllForExpense(expenseId))
    }

    suspend fun listForGroup(groupId: UUID): List<ExpenseResponse> = database.read {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("Group $groupId was not found")
        }
        val sharesByExpenseId = expenseShareRepository.findAllForGroup(groupId).groupBy { it.expense.id }
        expenseRepository.findAllForGroup(groupId).map { expense ->
            expense.toResponse(sharesByExpenseId[expense.id].orEmpty())
        }
    }
}

private fun ExpenseEntity.toResponse(shares: List<ExpenseShareEntity>) = ExpenseResponse(
    id = id,
    groupId = group.id,
    description = description,
    totalAmount = totalAmount,
    currency = currency,
    paidByUserId = paidBy.id,
    paidByDisplayName = paidBy.displayName,
    splitType = splitType.name,
    shares = shares.sortedBy { it.user.displayName }.map {
        ExpenseShareResponse(
            userId = it.user.id,
            displayName = it.user.displayName,
            amountOwed = it.amountOwed,
        )
    },
    createdAt = createdAt,
)
