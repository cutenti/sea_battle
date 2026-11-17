package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameEngineTest {
    private val engine = GameEngine(GameRulesValidatorImpl(), EloRatingCalculator())

    @Test
    fun `processMove returns INVALID_MOVE while the session is in setup`() {
        val session = session(Board(listOf(Ship(listOf(Position(0, 0))))), Board(emptyList()))

        val result = engine.processMove(session, session.player1.id, Position(0, 0))

        assertEquals(MoveResult.INVALID_MOVE, result)
        assertEquals(GameState.SETUP, session.state)
    }

    @Test
    fun `processMove returns KILLED when non-last ship is sunk`() {
        val session = started(
            opponent = Board(listOf(Ship(listOf(Position(0, 0))), Ship(listOf(Position(5, 5))))),
        )

        val result = engine.processMove(session, session.player1.id, Position(0, 0))

        assertEquals(MoveResult.KILLED, result)
        assertEquals(GameState.IN_PROGRESS, session.state)
        assertEquals(session.player1.id, session.currentTurnPlayerId)
    }

    @Test
    fun `processMove returns ALREADY_SHOT on repeating shot to the same cell`() {
        val session = started(opponent = Board(emptyList()))

        engine.processMove(session, session.player1.id, Position(0, 0))
        session.switchTurn()

        val result = engine.processMove(session, session.player1.id, Position(0, 0))

        assertEquals(MoveResult.ALREADY_SHOT, result)
        assertEquals(1000, session.player1.rating)
    }

    @Test
    fun `processMove returns GAME_OVER and updates elo when last ship is sunk`() {
        val session = started(opponent = Board(listOf(Ship(listOf(Position(0, 0))))))

        val result = engine.processMove(session, session.player1.id, Position(0, 0))

        assertEquals(MoveResult.GAME_OVER, result)
        assertEquals(GameState.FINISHED, session.state)
        assertEquals(session.player1.id, session.winnerId)
        assertEquals(1016, session.player1.rating)
        assertEquals(984, session.player2.rating)
    }

    @Test
    fun `processMove does not change rating again after the game is finished`() {
        val session = started(opponent = Board(listOf(Ship(listOf(Position(0, 0))))))
        engine.processMove(session, session.player1.id, Position(0, 0))

        val second = engine.processMove(session, session.player1.id, Position(1, 1))

        assertEquals(MoveResult.INVALID_MOVE, second)
        assertEquals(1016, session.player1.rating)
        assertEquals(984, session.player2.rating)
    }

    @Test
    fun `rating does not fall below zero`() {
        val winner = Player("p1", "P1", rating = 10)
        val loser = Player("p2", "P2", rating = 10)
        val session = GameSession(
            "g1",
            winner,
            loser,
            Board(emptyList()),
            Board(listOf(Ship(listOf(Position(0, 0))))),
        )
        session.begin()

        engine.processMove(session, winner.id, Position(0, 0))

        assertEquals(26, winner.rating)
        assertEquals(0, loser.rating)
    }

    private fun session(own: Board = Board(emptyList()), opponent: Board): GameSession =
        GameSession("g1", Player("p1", "P1"), Player("p2", "P2"), own, opponent)

    private fun started(opponent: Board): GameSession = session(opponent = opponent).also { it.begin() }
}
