package com.costsplit.api.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "app_users")
class UserEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "display_name", nullable = false, length = 100)
    var displayName: String,

    @Column(nullable = false, unique = true, length = 320)
    var email: String,

    @Column(name = "password_hash", length = 100)
    var passwordHash: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "expense_groups")
class ExpenseGroupEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 120)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    val owner: UserEntity,

    @Column(length = 40)
    var icon: String? = null,

    @Column(length = 7)
    var color: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = [UniqueConstraint(name = "uq_group_members_group_user", columnNames = ["group_id", "user_id"])],
)
class GroupMemberEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    val group: ExpenseGroupEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "joined_at", nullable = false, updatable = false)
    val joinedAt: Instant = Instant.now(),
)

enum class SplitType {
    EQUAL,
}

@Entity
@Table(name = "expenses")
class ExpenseEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    val group: ExpenseGroupEntity,

    @Column(nullable = false, length = 200)
    val description: String,

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    val totalAmount: BigDecimal,

    @Column(nullable = false, length = 3)
    val currency: String,

    @Column(nullable = false, length = 40)
    val category: String = "general",

    @Column(length = 500)
    val note: String? = null,

    @Column(name = "occurred_on", nullable = false)
    val occurredOn: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    val paidBy: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    val splitType: SplitType = SplitType.EQUAL,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "expense_shares",
    uniqueConstraints = [UniqueConstraint(name = "uq_expense_shares_expense_user", columnNames = ["expense_id", "user_id"])],
)
class ExpenseShareEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    val expense: ExpenseEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "amount_owed", nullable = false, precision = 19, scale = 2)
    val amountOwed: BigDecimal,
)

@Entity
@Table(name = "settlements")
class SettlementEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    val group: ExpenseGroupEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false)
    val fromUser: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    val toUser: UserEntity,

    @Column(nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,

    @Column(nullable = false, length = 3)
    val currency: String,

    @Column(length = 500)
    val note: String? = null,

    @Column(name = "settled_on", nullable = false)
    val settledOn: LocalDate = LocalDate.now(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
