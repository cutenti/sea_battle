package battleship.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameEngineTest {
    private lateinit var validator: GameRulesValidatorImpl
    private lateinit var engine: GameEngine

    @BeforeEach
    fun setup() {
        validator = GameRulesValidatorImpl()
        engine = GameEngine(validator)
    }

    @Test
    fun `processMove returns KILLED when non-last ship is sunk`() {
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        val b2 = Board(listOf(
            Ship(listOf(Position(0, 0))),
            Ship(listOf(Position(5, 5)))
        ))
        val session = GameSession("g1", p1, p2, Board(emptyList()), b2)

        val result = engine.processMove(session, p1.id, Position(0, 0))

        assertEquals(MoveResult.KILLED, result)
        assertEquals(GameState.IN_PROGRESS, session.state)
    }

    @Test
    fun `processMove returns ALREADY_SHOT on repeating shot to the same cell`() {
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        val session = GameSession("g1", p1, p2, Board(emptyList()), Board(emptyList()))

        engine.processMove(session, p1.id, Position(0, 0))
        session.switchTurn()

        val result = engine.processMove(session, p1.id, Position(0, 0))

        assertEquals(MoveResult.ALREADY_SHOT, result)
    }

    @Test
    fun `processMove returns GAME_OVER when last ship is sunk`() {
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        val b2 = Board(listOf(Ship(listOf(Position(0, 0)))))
        val session = GameSession("g1", p1, p2, Board(emptyList()), b2)

        val result = engine.processMove(session, p1.id, Position(0, 0))

        assertEquals(MoveResult.GAME_OVER, result)
        assertEquals(GameState.FINISHED, session.state)
        assertEquals(p1.id, session.winnerId)
    }
}