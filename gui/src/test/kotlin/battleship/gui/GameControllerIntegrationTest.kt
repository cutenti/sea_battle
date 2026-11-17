package battleship.gui

import battleship.core.BoardFactory
import battleship.core.EloRatingCalculator
import battleship.core.GameEngine
import battleship.core.GameRulesValidatorImpl
import battleship.core.GameService
import battleship.core.InMemoryGameRepository
import battleship.core.InMemoryPlayerRepository
import battleship.core.MoveResult
import battleship.core.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameControllerIntegrationTest {
    @Test
    fun `GUI controller starts a core game and sends a selected cell through the engine`() {
        val validator = GameRulesValidatorImpl()
        val players = InMemoryPlayerRepository()
        val service = GameService(
            players = players,
            games = InMemoryGameRepository(),
            engine = GameEngine(validator, EloRatingCalculator()),
            validator = validator,
        )
        val first = service.addPlayer("Alice")
        val second = service.addPlayer("Bob")
        val controller = GameController(
            service = service,
            validator = validator,
            boardFactory = BoardFactory::createValidClassicalBoard,
            nanoTime = { 5_000_000_000L },
        )

        val session = controller.startGame(first.id, second.id)
        val result = controller.shoot(Position(0, 0))

        assertEquals(MoveResult.HIT, result)
        assertEquals(MoveResult.HIT, session.moves.single().result)
        assertTrue(session.board2.shots.contains(Position(0, 0)))
        assertNotNull(controller.currentSession)
        assertEquals(0, controller.elapsedSeconds())
        assertEquals(2, controller.statistics().sumOf { it.gamesPlayed })
    }

    @Test
    fun `GUI controller reports win loss and persistent player rating from finished game`() {
        val validator = GameRulesValidatorImpl()
        val service = GameService(
            players = InMemoryPlayerRepository(),
            games = InMemoryGameRepository(),
            engine = GameEngine(validator, EloRatingCalculator()),
            validator = validator,
        )
        val first = service.addPlayer("Alice")
        val second = service.addPlayer("Bob")
        val controller = GameController(
            service,
            validator,
            BoardFactory::createValidClassicalBoard,
            sequenceOf(1_000_000_000L, 3_000_000_000L).iterator()::next,
        )
        val session = controller.startGame(first.id, second.id)

        session.board2.ships.flatMap { it.positions }.forEach { position ->
            val targetShip = session.board2.ships.single { position in it.positions }
            val lastShipCell = session.board2.ships.sumOf { ship ->
                (ship.positions.toSet() - ship.hits).size
            } == 1
            val expected = when {
                lastShipCell -> MoveResult.GAME_OVER
                targetShip.positions.size - targetShip.hits.size == 1 -> MoveResult.KILLED
                else -> MoveResult.HIT
            }
            assertEquals(expected, controller.shoot(position))
        }

        val statistics = controller.statistics().associateBy { it.player.id }
        assertEquals(1, statistics.getValue(first.id).wins)
        assertEquals(1, statistics.getValue(second.id).losses)
        assertTrue(statistics.getValue(first.id).player.rating > second.rating)
        assertEquals(2, controller.elapsedSeconds())
    }
}
