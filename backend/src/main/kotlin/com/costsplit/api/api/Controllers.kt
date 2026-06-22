package com.costsplit.api.api

import com.costsplit.api.service.BalanceService
import com.costsplit.api.service.ExpenseService
import com.costsplit.api.service.GroupService
import com.costsplit.api.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@Valid @RequestBody request: CreateUserRequest): UserResponse = userService.create(request)

    @GetMapping
    suspend fun list(): List<UserResponse> = userService.list()

    @GetMapping("/{userId}")
    suspend fun get(@PathVariable userId: UUID): UserResponse = userService.get(userId)
}

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val expenseService: ExpenseService,
    private val balanceService: BalanceService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@Valid @RequestBody request: CreateGroupRequest): GroupResponse = groupService.create(request)

    @GetMapping("/{groupId}")
    suspend fun get(@PathVariable groupId: UUID): GroupResponse = groupService.get(groupId)

    @PostMapping("/{groupId}/members")
    suspend fun addMember(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: AddGroupMemberRequest,
    ): GroupResponse = groupService.addMember(groupId, request)

    @PostMapping("/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createExpense(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: CreateExpenseRequest,
    ): ExpenseResponse = expenseService.create(groupId, request)

    @GetMapping("/{groupId}/expenses")
    suspend fun listExpenses(@PathVariable groupId: UUID): List<ExpenseResponse> = expenseService.listForGroup(groupId)

    @GetMapping("/{groupId}/balances")
    suspend fun getBalances(@PathVariable groupId: UUID): GroupBalancesResponse = balanceService.getGroupBalances(groupId)
}

@RestController
@RequestMapping("/api/v1/expenses")
class ExpenseController(
    private val expenseService: ExpenseService,
) {
    @GetMapping("/{expenseId}")
    suspend fun get(@PathVariable expenseId: UUID): ExpenseResponse = expenseService.get(expenseId)
}
