package battleship.core

import kotlin.math.abs

interface IGameRulesValidator {
    /** Клетка внутри поля 10×10. Координаты начинаются с нуля. */
    fun isValidPosition(pos: Position): Boolean

    /** Партия идёт, очередь этого игрока, клетка на поле. */
    fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean

    /** Законченная расстановка с полным классическим флотом. */
    fun validateBoard(board: Board): Boolean

    fun isValidShipShape(ship: Ship): Boolean

    /** Проверяет корабль относительно уже размещённых, пока флот ещё не завершён. */
    fun canPlaceShip(existingShips: List<Ship>, newShip: Ship): Boolean

    fun canPlaceShip(board: Board, ship: Ship): Boolean = canPlaceShip(board.ships, ship)
}

class GameRulesValidatorImpl : IGameRulesValidator {
    override fun isValidPosition(pos: Position): Boolean = pos.x in 0..9 && pos.y in 0..9

    override fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean {
        if (session.state != GameState.IN_PROGRESS) return false
        if (session.currentTurnPlayerId != playerId) return false
        return isValidPosition(pos)
    }

    override fun isValidShipShape(ship: Ship): Boolean {
        val positions = ship.positions
        if (positions.isEmpty() || positions.toSet().size != positions.size) return false
        if (positions.any { !isValidPosition(it) }) return false
        if (positions.size == 1) return true

        val xs = positions.map { it.x }
        val ys = positions.map { it.y }
        return when {
            ys.distinct().size == 1 -> xs.sorted().zipWithNext().all { (left, right) -> right - left == 1 }
            xs.distinct().size == 1 -> ys.sorted().zipWithNext().all { (top, bottom) -> bottom - top == 1 }
            else -> false
        }
    }

    override fun canPlaceShip(existingShips: List<Ship>, newShip: Ship): Boolean {
        if (!isValidShipShape(newShip)) return false
        return existingShips.none { existing -> shipsTouch(existing, newShip) }
    }

    override fun validateBoard(board: Board): Boolean {
        if (board.ships.size != 10 || board.ships.any { !isValidShipShape(it) }) return false
        for (i in board.ships.indices) {
            for (j in i + 1 until board.ships.size) {
                if (shipsTouch(board.ships[i], board.ships[j])) return false
            }
        }
        return board.ships.map { it.positions.size }.sorted() == listOf(1, 1, 1, 1, 2, 2, 2, 3, 3, 4)
    }

    /** Касание включает диагональ и наложение: расстояние Чебышёва не больше 1. */
    private fun shipsTouch(first: Ship, second: Ship): Boolean {
        for (one in first.positions) {
            for (other in second.positions) {
                if (abs(one.x - other.x) <= 1 && abs(one.y - other.y) <= 1) return true
            }
        }
        return false
    }
}
