package battleship.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameRulesValidatorTest {
    private val validator = GameRulesValidatorImpl()

    @Test
    fun `isValidPosition returns true for coordinates inside 10x10 board`() {
        assertTrue(validator.isValidPosition(Position(0, 0)))
        assertTrue(validator.isValidPosition(Position(9, 9)))
    }

    @Test
    fun `isValidPosition returns false for coordinates outside board`() {
        assertFalse(validator.isValidPosition(Position(-1, 5)))
        assertFalse(validator.isValidPosition(Position(10, 10)))
    }

    @Test
    fun `validateMove returns false if player acts out of turn`() {
        val p1 = Player("1", "P1")
        val p2 = Player("2", "P2")
        val session = GameSession("g1", p1, p2, Board(emptyList()), Board(emptyList()))
        
        // По умолчанию очередь первого игрока (p1). Пытаемся передать ход второго (p2).
        assertFalse(validator.validateMove(session, p2.id, Position(0, 0)))
    }
}