import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameEngineIntegrationTest {
    
    @Test
    fun `processMove changes turn on MISS`() {
        val engine = GameEngine(GameRulesValidatorImpl())
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        // Корабль на (0,0)
        val session = GameSession("g1", p1, p2, Board(emptyList()), Board(listOf(Ship(listOf(Position(0, 0))))))
        
        session.currentTurnPlayerId = p1.id

        // Выстрел мимо (1, 1)
        val result = engine.processMove(session, p1.id, Position(1, 1))

        assertEquals(MoveResult.MISS, result)
        assertEquals(p2.id, session.currentTurnPlayerId) // Ход перешел к игроку 2
    }

    @Test
    fun `processMove does not change turn on HIT`() {
        val engine = GameEngine(GameRulesValidatorImpl())
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        // Двухпалубный корабль
        val b2 = Board(listOf(Ship(listOf(Position(0, 0), Position(0, 1))))) 
        val session = GameSession("g1", p1, p2, Board(emptyList()), b2)
        
        val result = engine.processMove(session, p1.id, Position(0, 0))

        assertEquals(MoveResult.HIT, result)
        assertEquals(p1.id, session.currentTurnPlayerId) // Игрок 1 продолжает ход
    }
}