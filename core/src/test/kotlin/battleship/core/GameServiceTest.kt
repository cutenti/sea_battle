package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameServiceTest {

    @Test
    fun `startGame rejects an incomplete board and does not store a session`() {
        val games = InMemoryGameRepository()
        val service = service(games)
        val alice = Player("a", "Alice")
        val bob = Player("b", "Bob")

        assertThrows(InvalidGameSetupException::class.java) {
            service.startGame(alice, bob, Board(listOf(Ship(listOf(Position(0, 0))))), BoardFactory.createValidClassicalBoard())
        }
        assertTrue(games.findAll().isEmpty())
    }

    @Test
    fun `startGame rejects the same player on both sides`() {
        val service = service()
        val alice = Player("a", "Alice")
        val board = BoardFactory.createValidClassicalBoard()

        assertThrows(InvalidGameSetupException::class.java) {
            service.startGame(alice, alice, board, board)
        }
    }

    @Test
    fun `recordMove keeps a rejected shot in the journal and saves the session`() {
        val games = InMemoryGameRepository()
        val service = service(games)
        val session = service.startGame(
            Player("a", "Alice"),
            Player("b", "Bob"),
            BoardFactory.createValidClassicalBoard(),
            BoardFactory.createValidClassicalBoard(),
        )

        val result = service.recordMove(session, session.player2.id, Position(0, 0))

        assertEquals(MoveResult.INVALID_MOVE, result)
        assertEquals(1, session.moves.size)
        assertTrue(session.board2.shots.isEmpty())
        assertEquals(session, games.findById(session.id))
    }

    private fun service(games: IGameRepository = InMemoryGameRepository()): GameService {
        return GameService(
            InMemoryPlayerRepository(),
            games,
            GameEngine(GameRulesValidatorImpl()),
            GameRulesValidatorImpl(),
        )
    }
}
