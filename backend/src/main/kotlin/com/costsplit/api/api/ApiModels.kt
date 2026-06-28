package com.costsplit.api.api

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateUserRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val displayName: String,

    @field:NotBlank
    @field:Email
    @field:Size(max = 320)
    val email: String,
)

data class UserResponse(
    val id: UUID,
    val displayName: String,
    val email: String,
    val createdAt: Instant,
)

data class CreateGroupRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    val ownerUserId: UUID,

    val memberUserIds: Set<UUID> = emptySet(),

    @field:Size(max = 40)
    val icon: String? = null,

    @field:Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "must be a hex color like #4F46E5")
    val color: String? = null,
)

data class AddGroupMemberRequest(
    val userId: UUID,
)

data class GroupMemberResponse(
    val userId: UUID,
    val displayName: String,
    val email: String,
    val joinedAt: Instant,
)

data class GroupResponse(
    val id: UUID,
    val name: String,
    val ownerUserId: UUID,
    val icon: String?,
    val color: String?,
    val members: List<GroupMemberResponse>,
    val createdAt: Instant,
)

data class CreateExpenseRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val description: String,

    @field:DecimalMin(value = "0.01")
    val totalAmount: BigDecimal,

    @field:Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code")
    val currency: String,

    val paidByUserId: UUID,

    @field:NotEmpty
    val participantUserIds: Set<UUID>,

    @field:Size(max = 40)
    val category: String = "general",

    @field:Size(max = 500)
    val note: String? = null,

    val occurredOn: LocalDate? = null,
)

data class ExpenseShareResponse(
    val userId: UUID,
    val displayName: String,
    val amountOwed: BigDecimal,
)

data class ExpenseResponse(
    val id: UUID,
    val groupId: UUID,
    val description: String,
    val totalAmount: BigDecimal,
    val currency: String,
    val paidByUserId: UUID,
    val paidByDisplayName: String,
    val splitType: String,
    val category: String,
    val note: String?,
    val occurredOn: LocalDate,
    val shares: List<ExpenseShareResponse>,
    val createdAt: Instant,
)

data class CreateSettlementRequest(
    val fromUserId: UUID,
    val toUserId: UUID,

    @field:DecimalMin(value = "0.01")
    val amount: BigDecimal,

    @field:Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code")
    val currency: String,

    @field:Size(max = 500)
    val note: String? = null,

    val settledOn: LocalDate? = null,
)

data class SettlementResponse(
    val id: UUID,
    val groupId: UUID,
    val fromUserId: UUID,
    val fromDisplayName: String,
    val toUserId: UUID,
    val toDisplayName: String,
    val amount: BigDecimal,
    val currency: String,
    val note: String?,
    val settledOn: LocalDate,
    val createdAt: Instant,
)

data class MemberBalanceResponse(
    val userId: UUID,
    val displayName: String,
    val netAmount: BigDecimal,
)

data class SuggestedSettlementResponse(
    val fromUserId: UUID,
    val toUserId: UUID,
    val amount: BigDecimal,
)

data class CurrencyBalanceResponse(
    val currency: String,
    val members: List<MemberBalanceResponse>,
    val suggestedSettlements: List<SuggestedSettlementResponse>,
)

data class GroupBalancesResponse(
    val groupId: UUID,
    val balances: List<CurrencyBalanceResponse>,
)
