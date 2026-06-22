package com.costsplit.api.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class EqualSplitCalculatorTest {
    private val calculator = EqualSplitCalculator()

    @Test
    fun `splits evenly and assigns rounding cents deterministically`() {
        val users = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

        val result = calculator.split(BigDecimal("10.00"), users)

        assertEquals(BigDecimal("10.00"), result.values.fold(BigDecimal.ZERO, BigDecimal::add))
        assertEquals(listOf(BigDecimal("3.34"), BigDecimal("3.33"), BigDecimal("3.33")), result.values.sortedDescending())
    }

    @Test
    fun `does not charge a duplicated participant twice`() {
        val user = UUID.randomUUID()

        val result = calculator.split(BigDecimal("12.50"), listOf(user, user))

        assertEquals(mapOf(user to BigDecimal("12.50")), result)
    }

    @Test
    fun `rejects amounts with more than two decimal places`() {
        assertThrows<InvalidRequestException> {
            calculator.split(BigDecimal("10.001"), listOf(UUID.randomUUID()))
        }
    }

    @Test
    fun `rejects an empty participant list`() {
        assertThrows<InvalidRequestException> {
            calculator.split(BigDecimal("10.00"), emptyList())
        }
    }
}

