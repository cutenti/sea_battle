package battleship.core

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * На сколько очков вырастет рейтинг победителя.
 * Проигравший теряет столько же; пол «не ниже нуля» применяет движок, а не калькулятор.
 */
interface IRatingCalculator {
    fun calculateRatingChange(winner: Player, loser: Player): Int
}

/**
 * Упрощённый Эло с коэффициентом K = 32.
 *
 * Ожидаемый результат победителя:
 * E = 1 / (1 + 10 ^ ((R проигравшего − R победителя) / 400))
 * Прирост = round(K * (1 − E)).
 *
 * Оба рейтинга 1000: E = 0.5, прирост = 16.
 * Победитель 1200 против 1000: E ≈ 0.760, прирост = 8.
 * Сумма двух приростов при зеркальном исходе равна K, пока округление не сдвинет её на 1.
 */
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
