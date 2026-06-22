package com.costsplit.api.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun existsByEmail(email: String): Boolean
    fun findAllByOrderByDisplayNameAsc(): List<UserEntity>
}

interface ExpenseGroupRepository : JpaRepository<ExpenseGroupEntity, UUID>

interface GroupMemberRepository : JpaRepository<GroupMemberEntity, UUID> {
    fun existsByGroupIdAndUserId(groupId: UUID, userId: UUID): Boolean

    @Query("select member from GroupMemberEntity member join fetch member.user where member.group.id = :groupId order by member.user.displayName")
    fun findMembersWithUsers(groupId: UUID): List<GroupMemberEntity>
}

interface ExpenseRepository : JpaRepository<ExpenseEntity, UUID> {
    @Query("select expense from ExpenseEntity expense join fetch expense.paidBy where expense.group.id = :groupId order by expense.createdAt desc")
    fun findAllForGroup(groupId: UUID): List<ExpenseEntity>
}

interface ExpenseShareRepository : JpaRepository<ExpenseShareEntity, UUID> {
    @Query("select share from ExpenseShareEntity share join fetch share.user where share.expense.id = :expenseId order by share.user.displayName")
    fun findAllForExpense(expenseId: UUID): List<ExpenseShareEntity>

    @Query("select share from ExpenseShareEntity share join fetch share.user join fetch share.expense expense join fetch expense.paidBy where expense.group.id = :groupId")
    fun findAllForGroup(groupId: UUID): List<ExpenseShareEntity>
}
