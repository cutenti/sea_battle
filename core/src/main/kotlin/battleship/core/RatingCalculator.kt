package battleship.core

import kotlin.math.pow
import kotlin.math.roundToInt

interface IRatingCalculator {
    fun calculateRatingChange(winner: Player, loser: Player): Int
}

/** Elo rating change for a winner with a K-factor of 32. */
class EloRatingCalculator(private val k: Int = 32) : IRatingCalculator {
    init {
        require(k > 0) { "Коэффициент K должен быть положительным" }
    }

    override fun calculateRatingChange(winner: Player, loser: Player): Int {
        val exponent = (loser.rating - winner.rating) / 400.0
        val expected = 1.0 / (1.0 + 10.0.pow(exponent))
        return (k * (1.0 - expected)).roundToInt()
    }
}

/** Fixed ten-point rating change, retained for clients that use the stage-three policy. */
class FixedRatingCalculator : IRatingCalculator {
    override fun calculateRatingChange(winner: Player, loser: Player): Int = 10
}
