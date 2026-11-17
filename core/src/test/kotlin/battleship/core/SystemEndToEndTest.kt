package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SystemEndToEndTest {

    @Test
    fun `full fleet is sunk through the service and the rating is stored`() {
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val validator = GameRulesValidatorImpl()
        val service = GameService(players, games, GameEngine(validator), validator)
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")
        players.save(alice)
        players.save(bob)

        val session = service.startGame(
            alice,
            bob,
            BoardFactory.createValidClassicalBoard(),
            BoardFactory.createValidClassicalBoard(),
        )
        assertEquals(GameState.IN_PROGRESS, session.state)

        val cells = session.board2.ships.flatMap { it.positions }
        var last = MoveResult.MISS
        for (cell in cells) {
            assertEquals(alice.id, session.currentTurnPlayerId)
            last = service.recordMove(session, alice.id, cell)
        }

        assertEquals(MoveResult.GAME_OVER, last)
        assertEquals(GameState.FINISHED, session.state)
        assertEquals(alice.id, session.winnerId)
        assertEquals(1016, alice.rating)
        assertEquals(984, bob.rating)
        assertEquals(cells.size, session.moves.size)
        assertEquals(session, games.findById(session.id))
        assertEquals(1016, players.findById(alice.id)?.rating)
        assertTrue(session.board2.isAllShipsSunk())
    }

    @Test
    fun `invalid out of turn and repeated shots stay in the journal without passing the turn`() {
        val validator = GameRulesValidatorImpl()
        val service = GameService(
            InMemoryPlayerRepository(),
            InMemoryGameRepository(),
            GameEngine(validator),
            validator,
        )
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")
        val session = service.startGame(
            alice,
            bob,
            BoardFactory.createValidClassicalBoard(),
            BoardFactory.createValidClassicalBoard(),
        )

        assertEquals(MoveResult.INVALID_MOVE, service.recordMove(session, bob.id, Position(5, 5)))
        assertEquals(alice.id, session.currentTurnPlayerId)

        assertEquals(MoveResult.INVALID_MOVE, service.recordMove(session, alice.id, Position(10, 10)))
        assertTrue(session.board2.shots.isEmpty())

        assertEquals(MoveResult.MISS, service.recordMove(session, alice.id, Position(1, 0)))
        assertEquals(bob.id, session.currentTurnPlayerId)

        assertEquals(MoveResult.MISS, service.recordMove(session, bob.id, Position(1, 1)))
        assertEquals(alice.id, session.currentTurnPlayerId)

        assertEquals(MoveResult.ALREADY_SHOT, service.recordMove(session, alice.id, Position(1, 0)))
        assertEquals(alice.id, session.currentTurnPlayerId)
        assertEquals(5, session.moves.size)
        assertEquals(1000, alice.rating)
    }
}
