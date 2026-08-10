import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShipTest {

    @Test
    fun `isSunk returns false when not all positions are hit`() {
        val ship = Ship(listOf(Position(0, 0), Position(0, 1)))
        
        ship.hits.add(Position(0, 0)) // Ранили, но не убили
        
        assertFalse(ship.isSunk())
    }

    @Test
    fun `isSunk returns true when all positions are hit`() {
        val ship = Ship(listOf(Position(0, 0), Position(0, 1)))
        
        ship.hits.add(Position(0, 0))
        ship.hits.add(Position(0, 1)) // Попали во все палубы
        
        assertTrue(ship.isSunk())
    }
}

class BoardTest {

    @Test
    fun `isAllShipsSunk returns false if at least one ship is still alive`() {
        val ship1 = Ship(listOf(Position(0, 0)))
        val ship2 = Ship(listOf(Position(1, 1)))
        val board = Board(listOf(ship1, ship2))
        
        ship1.hits.add(Position(0, 0)) // Первый корабль потоплен, второй цел
        
        assertFalse(board.isAllShipsSunk())
    }

    @Test
    fun `isAllShipsSunk returns true when all ships are sunk`() {
        val ship1 = Ship(listOf(Position(0, 0)))
        val ship2 = Ship(listOf(Position(1, 1)))
        val board = Board(listOf(ship1, ship2))
        
        ship1.hits.add(Position(0, 0))
        ship2.hits.add(Position(1, 1))
        
        assertTrue(board.isAllShipsSunk())
    }
}