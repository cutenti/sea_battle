package battleship.core

import kotlin.math.abs

interface IGameRulesValidator {
    fun isValidPosition(pos: Position): Boolean
    fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean
    fun validateBoard(board: Board): Boolean
}

class GameRulesValidatorImpl : IGameRulesValidator {
    override fun isValidPosition(pos: Position): Boolean =
        pos.x in 0..9 && pos.y in 0..9

    override fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean {
        if (session.state != GameState.IN_PROGRESS) return false
        if (session.currentTurnPlayerId != playerId) return false
        return isValidPosition(pos)
    }

    override fun validateBoard(board: Board): Boolean {
        val allPositions = board.ships.flatMap { it.positions }
        
        // 1. Проверка выхода за границы
        if (allPositions.any { !isValidPosition(it) }) return false
        
        // 2. Проверка наложения кораблей друг на друга
        if (allPositions.toSet().size != allPositions.size) return false
        
        // 3. Проверка соприкосновений
        for (i in board.ships.indices) {
            for (j in i + 1 until board.ships.size) {
                if (shipsTouch(board.ships[i], board.ships[j])) return false
            }
        }
        
        // 4. Проверка количества и размеров кораблей (1x4, 2x3, 3x2, 4x1)
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