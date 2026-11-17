package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RatingCalculatorTest {

    private val calculator = EloRatingCalculator()

    @Test
    fun `equal ratings give a 16 point swing`() {
        val change = calculator.calculateRatingChange(Player("w", "W", 1000), Player("l", "L", 1000))
        assertEquals(16, change)
    }

    @Test
    fun `favorite who wins gains 8 points from 1200 against 1000`() {
        val change = calculator.calculateRatingChange(Player("w", "W", 1200), Player("l", "L", 1000))
        assertEquals(8, change)
    }

    @Test
    fun `underdog who wins gains 24 points from 1000 against 1200`() {
        val change = calculator.calculateRatingChange(Player("w", "W", 1000), Player("l", "L", 1200))
        assertEquals(24, change)
    }
}
