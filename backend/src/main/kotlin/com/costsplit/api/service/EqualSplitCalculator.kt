package com.costsplit.api.service

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Component
class EqualSplitCalculator {
    fun split(totalAmount: BigDecimal, participantIds: Collection<UUID>): Map<UUID, BigDecimal> {
        if (participantIds.isEmpty()) {
            throw InvalidRequestException("At least one participant is required")
        }

        val normalizedTotal = try {
            totalAmount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
        } catch (_: ArithmeticException) {
            throw InvalidRequestException("totalAmount can have at most 2 decimal places")
        }
        if (normalizedTotal <= BigDecimal.ZERO) {
            throw InvalidRequestException("totalAmount must be greater than zero")
        }

        val sortedIds = participantIds.distinct().sorted()
        val participantCount = BigDecimal.valueOf(sortedIds.size.toLong())
        val baseShare = normalizedTotal.divide(participantCount, MONEY_SCALE, RoundingMode.DOWN)
        var remainingCents = normalizedTotal.subtract(baseShare.multiply(participantCount)).movePointRight(MONEY_SCALE).intValueExact()

        return sortedIds.associateWith {
            if (remainingCents > 0) {
                remainingCents--
                baseShare.add(ONE_CENT)
            } else {
                baseShare
            }
        }
    }

    companion object {
        const val MONEY_SCALE = 2
        private val ONE_CENT = BigDecimal("0.01")
    }
}

