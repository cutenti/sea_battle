package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameEngineIntegrationTest {

    @Test
    fun `processMove changes turn on MISS`() {
        val engine = GameEngine(GameRulesValidatorImpl())
        val session = started(Board(listOf(Ship(listOf(Position(0, 0))))) )

        val result = engine.processMove(session, session.player1.id, Position(1, 1))

        assertEquals(MoveResult.MISS, result)
        assertEquals(session.player2.id, session.currentTurnPlayerId)
    }

    @Test
    fun `processMove does not change turn on HIT`() {
        val engine = GameEngine(GameRulesValidatorImpl())
        val session = started(Board(listOf(Ship(listOf(Position(0, 0), Position(0, 1))))) )

        val result = engine.processMove(session, session.player1.id, Position(0, 0))

        assertEquals(MoveResult.HIT, result)
        assertEquals(session.player1.id, session.currentTurnPlayerId)
    }

    private fun started(opponent: Board): GameSession {
        val session = GameSession(
            "g1",
            Player("p1", "P1"),
            Player("p2", "P2"),
            Board(emptyList()),
            opponent,
        )
        session.begin()
        return session
    }
}
