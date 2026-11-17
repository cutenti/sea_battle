package battleship.core

import kotlin.math.abs

interface IGameRulesValidator {
    fun isValidPosition(pos: Position): Boolean
    fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean
    fun validateBoard(board: Board): Boolean
    fun isValidShipShape(ship: Ship): Boolean
    fun canPlaceShip(board: Board, ship: Ship): Boolean
}

class GameRulesValidatorImpl : IGameRulesValidator {
    override fun isValidPosition(pos: Position): Boolean =
        pos.x in 0..9 && pos.y in 0..9

    override fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean {
        if (session.state != GameState.IN_PROGRESS) return false
        if (session.currentTurnPlayerId != playerId) return false
        return isValidPosition(pos)
    }

    override fun isValidShipShape(ship: Ship): Boolean {
        val positions = ship.positions
        if (positions.isEmpty()) return false
        if (positions.toSet().size != positions.size) return false
        if (positions.any { !isValidPosition(it) }) return false

        val xs = positions.map { it.x }.distinct()
        val ys = positions.map { it.y }.distinct()

        if (xs.size == 1) {
            val sortedYs = positions.map { it.y }.sorted()
            return sortedYs.zipWithNext().all { (previous, next) -> next - previous == 1 }
        }

        if (ys.size == 1) {
            val sortedXs = positions.map { it.x }.sorted()
            return sortedXs.zipWithNext().all { (previous, next) -> next - previous == 1 }
        }

        return false
    }

    override fun canPlaceShip(board: Board, ship: Ship): Boolean {
        if (!isValidShipShape(ship)) return false
        if (board.ships.any { existing -> existing.positions.any { it in ship.positions } }) return false
        return board.ships.none { existing -> shipsTouch(existing, ship) }
    }

    override fun validateBoard(board: Board): Boolean {
        if (board.ships.size != 10) return false

        val allPositions = board.ships.flatMap { it.positions }
        if (allPositions.any { !isValidPosition(it) }) return false
        if (allPositions.toSet().size != allPositions.size) return false

        if (board.ships.any { !isValidShipShape(it) }) return false

        for (i in board.ships.indices) {
            for (j in i + 1 until board.ships.size) {
                if (shipsTouch(board.ships[i], board.ships[j])) return false
            }
        }

        val sizes = board.ships.map { it.positions.size }.sorted()
        val expected = listOf(1, 1, 1, 1, 2, 2, 2, 3, 3, 4)
        return sizes == expected
    }

    private fun shipsTouch(s1: Ship, s2: Ship): Boolean {
        for (p1 in s1.positions) {
            for (p2 in s2.positions) {
                if (abs(p1.x - p2.x) <= 1 && abs(p1.y - p2.y) <= 1) return true
            }
        }
        return false
    }
}