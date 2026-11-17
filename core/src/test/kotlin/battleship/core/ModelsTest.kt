package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShipTest {

    @Test
    fun `isSunk returns false when not all positions are hit`() {
        val ship = Ship(listOf(Position(0, 0), Position(0, 1)))
        ship.addHit(Position(0, 0))
        assertFalse(ship.isSunk())
    }

    @Test
    fun `isSunk returns true when all positions are hit`() {
        val ship = Ship(listOf(Position(0, 0), Position(0, 1)))
        ship.addHit(Position(0, 0))
        ship.addHit(Position(0, 1))
        assertTrue(ship.isSunk())
    }

    @Test
    fun `addHit ignores a cell that does not belong to the ship`() {
        val ship = Ship(listOf(Position(0, 0)))
        ship.addHit(Position(1, 1))
        assertTrue(ship.hits.isEmpty())
        assertFalse(ship.isSunk())
    }
}

class BoardTest {

    @Test
    fun `isAllShipsSunk returns false if at least one ship is still alive`() {
        val ship1 = Ship(listOf(Position(0, 0)))
        val ship2 = Ship(listOf(Position(1, 1)))
        val board = Board(listOf(ship1, ship2))
        ship1.addHit(Position(0, 0))
        assertFalse(board.isAllShipsSunk())
    }

    @Test
    fun `isAllShipsSunk returns true when all ships are sunk`() {
        val ship1 = Ship(listOf(Position(0, 0)))
        val ship2 = Ship(listOf(Position(1, 1)))
        val board = Board(listOf(ship1, ship2))
        ship1.addHit(Position(0, 0))
        ship2.addHit(Position(1, 1))
        assertTrue(board.isAllShipsSunk())
    }

    @Test
    fun `empty board is not treated as already destroyed`() {
        assertFalse(Board(emptyList()).isAllShipsSunk())
    }
}

class GameSessionTest {

    @Test
    fun `new session waits in setup until begin`() {
        val session = session()
        assertEquals(GameState.SETUP, session.state)
        session.begin()
        assertEquals(GameState.IN_PROGRESS, session.state)
        assertThrows(IllegalStateException::class.java) { session.begin() }
    }

    @Test
    fun `journal keeps attempts in order`() {
        val session = session()
        val first = MoveRecord("p1", Position(0, 0), MoveResult.MISS)
        val second = MoveRecord("p2", Position(1, 1), MoveResult.HIT)
        session.addMove(first)
        session.addMove(second)
        assertEquals(listOf(first, second), session.moves)
    }

    private fun session(): GameSession {
        return GameSession("g1", Player("p1", "P1"), Player("p2", "P2"), Board(emptyList()), Board(emptyList()))
    }
}

class PlayerTest {

    @Test
    fun `rating cannot be created or updated below zero`() {
        assertThrows(IllegalArgumentException::class.java) { Player("p", "P", rating = -1) }
        val player = Player("p", "P", rating = 0)
        assertThrows(IllegalArgumentException::class.java) { player.updateRating(-5) }
    }
}
