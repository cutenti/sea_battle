package battleship.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
        val session = startedSession()
        assertFalse(validator.validateMove(session, session.player2.id, Position(0, 0)))
    }

    @Test
    fun `validateMove returns false while the match is still in setup`() {
        val session = session()
        assertTrue(session.state == GameState.SETUP)
        assertFalse(validator.validateMove(session, session.player1.id, Position(0, 0)))
    }

    @Test
    fun `canPlaceShip accepts a straight ship and validateBoard rejects a partial fleet`() {
        val ship = Ship(listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0)))

        assertTrue(validator.canPlaceShip(emptyList(), ship))
        assertFalse(validator.validateBoard(Board(listOf(ship))))
    }

    @Test
    fun `canPlaceShip accepts a ship written in reverse order`() {
        val ship = Ship(listOf(Position(3, 0), Position(2, 0), Position(1, 0), Position(0, 0)))
        assertTrue(validator.canPlaceShip(emptyList(), ship))
    }

    @Test
    fun `canPlaceShip rejects touch including diagonal and accepts a one-cell gap`() {
        val placed = Ship(listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0)))

        assertFalse(validator.canPlaceShip(listOf(placed), Ship(listOf(Position(0, 1)))))
        assertFalse(validator.canPlaceShip(listOf(placed), Ship(listOf(Position(4, 1)))))
        assertTrue(validator.canPlaceShip(listOf(placed), Ship(listOf(Position(0, 2)))))
    }

    @Test
    fun `canPlaceShip rejects broken shapes and cells outside the board`() {
        assertFalse(validator.canPlaceShip(emptyList(), Ship(listOf(Position(0, 0), Position(0, 1), Position(1, 1)))))
        assertFalse(validator.canPlaceShip(emptyList(), Ship(listOf(Position(0, 0), Position(1, 1)))))
        assertFalse(validator.canPlaceShip(emptyList(), Ship(listOf(Position(0, 0), Position(0, 2)))))
        assertFalse(validator.canPlaceShip(emptyList(), Ship(listOf(Position(0, 0), Position(0, 0)))))
        assertFalse(validator.canPlaceShip(emptyList(), Ship(listOf(Position(9, 9), Position(10, 9)))))
        assertFalse(validator.canPlaceShip(emptyList(), Ship(emptyList())))
    }

    @Test
    fun `validateBoard accepts the classical fleet`() {
        assertTrue(validator.validateBoard(BoardFactory.createValidClassicalBoard()))
    }

    @Test
    fun `validateBoard rejects an L-shaped ship even when the fleet sizes match`() {
        val ships = BoardFactory.createValidClassicalBoard().ships.toMutableList()
        ships[1] = Ship(listOf(Position(2, 8), Position(2, 9), Position(3, 9)))
        assertFalse(validator.validateBoard(Board(ships)))
    }

    @Test
    fun `validateBoard rejects a diagonal ship`() {
        val ships = BoardFactory.createValidClassicalBoard().ships.toMutableList()
        ships[4] = Ship(listOf(Position(8, 8), Position(9, 9)))
        assertFalse(validator.validateBoard(Board(ships)))
    }

    @Test
    fun `validateBoard rejects a ship with a gap`() {
        val ships = BoardFactory.createValidClassicalBoard().ships.toMutableList()
        ships[4] = Ship(listOf(Position(9, 3), Position(9, 8)))
        assertFalse(validator.validateBoard(Board(ships)))
    }

    @Test
    fun `validateBoard rejects a cell outside the board`() {
        val ships = BoardFactory.createValidClassicalBoard().ships.toMutableList()
        ships[9] = Ship(listOf(Position(10, 6)))
        assertFalse(validator.validateBoard(Board(ships)))
    }

    private fun session(): GameSession {
        val first = Player("1", "P1")
        val second = Player("2", "P2")
        return GameSession("g1", first, second, Board(emptyList()), Board(emptyList()))
    }

    private fun startedSession(): GameSession = session().also { it.begin() }
}
