package battleship.core

import kotlin.math.abs

interface IGameRulesValidator {
    /** Клетка внутри поля 10×10. Координаты начинаются с нуля. */
    fun isValidPosition(pos: Position): Boolean

    /** Партия идёт, очередь этого игрока, клетка на поле. */
    fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean

    /**
     * Законченная расстановка.
     * Форма каждого корабля, границы, нет наложений и касаний,
     * длины ровно 4, 3, 3, 2, 2, 2, 1, 1, 1, 1.
     */
    fun validateBoard(board: Board): Boolean

    /**
     * Корабль можно поставить рядом с уже стоящими.
     * Полный комплект здесь не требуется: проверкой пользуется пошаговая генерация.
     */
    fun canPlaceShip(existingShips: List<Ship>, newShip: Ship): Boolean
}

class GameRulesValidatorImpl : IGameRulesValidator {
    override fun isValidPosition(pos: Position): Boolean =
        pos.x in 0..9 && pos.y in 0..9

    override fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean {
        if (session.state != GameState.IN_PROGRESS) return false
        if (session.currentTurnPlayerId != playerId) return false
        return isValidPosition(pos)
    }

    override fun canPlaceShip(existingShips: List<Ship>, newShip: Ship): Boolean {
        if (!isValidShipShape(newShip)) return false
        if (newShip.positions.any { !isValidPosition(it) }) return false
        return existingShips.none { shipsTouch(it, newShip) }
    }

    override fun validateBoard(board: Board): Boolean {
        if (board.ships.any { !isValidShipShape(it) }) return false

        val allPositions = board.ships.flatMap { it.positions }
        if (allPositions.any { !isValidPosition(it) }) return false
        if (allPositions.toSet().size != allPositions.size) return false

        for (i in board.ships.indices) {
            for (j in i + 1 until board.ships.size) {
                if (shipsTouch(board.ships[i], board.ships[j])) return false
            }
        }

        val sizes = board.ships.map { it.positions.size }.sorted()
        val expected = listOf(1, 1, 1, 1, 2, 2, 2, 3, 3, 4)
        return sizes == expected
    }

    /**
     * Клетки идут подряд по одной горизонтали или одной вертикали.
     * Порядок в списке не важен: меняющаяся координата сортируется,
     * и соседние значения должны отличаться ровно на 1.
     * Так отсекаются уголок, диагональ, дырка и повтор клетки.
     */
    private fun isValidShipShape(ship: Ship): Boolean {
        val positions = ship.positions
        if (positions.isEmpty()) return false
        if (positions.size == 1) return true

        val xs = positions.map { it.x }
        val ys = positions.map { it.y }
        val horizontal = ys.distinct().size == 1
        val vertical = xs.distinct().size == 1

        return when {
            horizontal -> xs.sorted().zipWithNext().all { (left, right) -> right - left == 1 }
            vertical -> ys.sorted().zipWithNext().all { (left, right) -> right - left == 1 }
            else -> false
        }
    }

    /** Касание включает диагональ и наложение: расстояние Чебышёва не больше 1. */
    private fun shipsTouch(s1: Ship, s2: Ship): Boolean {
        for (p1 in s1.positions) {
            for (p2 in s2.positions) {
                if (abs(p1.x - p2.x) <= 1 && abs(p1.y - p2.y) <= 1) return true
            }
        }
        return false
    }
}
