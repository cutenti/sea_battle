package battleship.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameEngineTest {
    private lateinit var validator: IGameRulesValidator
    private lateinit var engine: GameEngine

    @BeforeEach
    fun setup() {
        validator = PermissiveBoardRulesValidator()
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

        assertTrue(engine.startGame(session))
        val result = engine.processMove(session, p1.id, Position(0, 0))

        assertEquals(MoveResult.KILLED, result)
        assertEquals(GameState.IN_PROGRESS, session.state)
    }

    @Test
    fun `processMove returns ALREADY_SHOT on repeating shot to the same cell`() {
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        val session = GameSession("g1", p1, p2, Board(emptyList()), Board(emptyList()))

        assertTrue(engine.startGame(session))
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

        assertTrue(engine.startGame(session))
        val result = engine.processMove(session, p1.id, Position(0, 0))

        assertEquals(MoveResult.GAME_OVER, result)
        assertEquals(GameState.FINISHED, session.state)
        assertEquals(p1.id, session.winnerId)
        assertEquals(1010, p1.rating)
        assertEquals(990, p2.rating)
        assertEquals(MoveResult.GAME_OVER, session.moves.single().result)
    }

    @Test
    fun `startGame rejects invalid boards and leaves session in setup`() {
        val strictEngine = GameEngine(GameRulesValidatorImpl())
        val session = GameSession(
            "g1",
            Player("p1", "P1"),
            Player("p2", "P2"),
            Board(emptyList()),
            Board(emptyList())
        )

        assertFalse(strictEngine.startGame(session))
        assertEquals(GameState.SETUP, session.state)
        assertEquals(MoveResult.INVALID_MOVE, strictEngine.processMove(session, "p1", Position(0, 0)))
    }

    @Test
    fun `startGame validates both boards before starting`() {
        val strictEngine = GameEngine(GameRulesValidatorImpl())
        val session = GameSession(
            "g1",
            Player("p1", "P1"),
            Player("p2", "P2"),
            BoardFactory.createValidClassicalBoard(),
            Board(emptyList())
        )

        assertFalse(strictEngine.startGame(session))
        assertEquals(GameState.SETUP, session.state)
    }

    @Test
    fun `processMove adds invalid attempts to the move journal`() {
        val p1 = Player("p1", "P1")
        val p2 = Player("p2", "P2")
        val session = GameSession("g1", p1, p2, Board(emptyList()), Board(emptyList()))
        assertTrue(engine.startGame(session))

        assertEquals(MoveResult.INVALID_MOVE, engine.processMove(session, p2.id, Position(0, 0)))
        assertEquals(MoveRecord(p2.id, Position(0, 0), MoveResult.INVALID_MOVE), session.moves.single())
    }
}