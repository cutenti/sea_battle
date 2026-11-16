package battleship.core

interface IRatingCalculator {
    fun calculateRatingChange(winner: Player, loser: Player): Int
}

class FixedRatingCalculator : IRatingCalculator {
    override fun calculateRatingChange(winner: Player, loser: Player): Int = 10
}
