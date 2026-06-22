package com.costsplit.api.service

import com.costsplit.api.api.CurrencyBalanceResponse
import com.costsplit.api.api.GroupBalancesResponse
import com.costsplit.api.api.MemberBalanceResponse
import com.costsplit.api.api.SuggestedSettlementResponse
import com.costsplit.api.domain.ExpenseGroupRepository
import com.costsplit.api.domain.ExpenseRepository
import com.costsplit.api.domain.ExpenseShareRepository
import com.costsplit.api.domain.GroupMemberRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class BalanceService(
    private val groupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseShareRepository: ExpenseShareRepository,
    private val database: CoroutineDatabaseExecutor,
) {
    suspend fun getGroupBalances(groupId: UUID): GroupBalancesResponse = coroutineScope {
        val groupExists = async {
            database.read { groupRepository.existsById(groupId) }
        }
        val membersDeferred = async {
            database.read { groupMemberRepository.findMembersWithUsers(groupId) }
        }
        val expensesDeferred = async {
            database.read { expenseRepository.findAllForGroup(groupId) }
        }
        val sharesDeferred = async {
            database.read { expenseShareRepository.findAllForGroup(groupId) }
        }

        if (!groupExists.await()) {
            throw NotFoundException("Group $groupId was not found")
        }

        val members = membersDeferred.await()
        val expenses = expensesDeferred.await()
        val shares = sharesDeferred.await()
        val displayNames = members.associate { it.user.id to it.user.displayName }
        val currencies = expenses.map { it.currency }.distinct().sorted()

        val currencyBalances = currencies.map { currency ->
            val netByUserId = members.associate { it.user.id to ZERO }.toMutableMap()
            expenses.filter { it.currency == currency }.forEach { expense ->
                netByUserId.compute(expense.paidBy.id) { _, current -> current!!.add(expense.totalAmount) }
            }
            shares.filter { it.expense.currency == currency }.forEach { share ->
                netByUserId.compute(share.user.id) { _, current -> current!!.subtract(share.amountOwed) }
            }

            CurrencyBalanceResponse(
                currency = currency,
                members = netByUserId.entries
                    .sortedBy { displayNames.getValue(it.key) }
                    .map { (userId, amount) ->
                        MemberBalanceResponse(
                            userId = userId,
                            displayName = displayNames.getValue(userId),
                            netAmount = amount,
                        )
                    },
                suggestedSettlements = calculateSettlements(netByUserId),
            )
        }

        GroupBalancesResponse(groupId = groupId, balances = currencyBalances)
    }

    private fun calculateSettlements(netByUserId: Map<UUID, BigDecimal>): List<SuggestedSettlementResponse> {
        val debtors = netByUserId
            .filterValues { it < ZERO }
            .map { MutableBalance(it.key, it.value.abs()) }
            .sortedBy { it.userId }
        val creditors = netByUserId
            .filterValues { it > ZERO }
            .map { MutableBalance(it.key, it.value) }
            .sortedBy { it.userId }

        val settlements = mutableListOf<SuggestedSettlementResponse>()
        var debtorIndex = 0
        var creditorIndex = 0
        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val amount = debtor.amount.min(creditor.amount)
            if (amount > ZERO) {
                settlements += SuggestedSettlementResponse(
                    fromUserId = debtor.userId,
                    toUserId = creditor.userId,
                    amount = amount,
                )
            }

            debtor.amount = debtor.amount.subtract(amount)
            creditor.amount = creditor.amount.subtract(amount)
            if (debtor.amount.compareTo(ZERO) == 0) debtorIndex++
            if (creditor.amount.compareTo(ZERO) == 0) creditorIndex++
        }
        return settlements
    }

    private data class MutableBalance(
        val userId: UUID,
        var amount: BigDecimal,
    )

    companion object {
        private val ZERO = BigDecimal("0.00")
    }
}
