package battleship.persistence

import battleship.core.BoardFactory
import battleship.core.EloRatingCalculator
import battleship.core.GameEngine
import battleship.core.GameRulesValidatorImpl
import battleship.core.GameService
import battleship.core.GameState
import battleship.core.MoveResult
import battleship.core.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SqliteRepositoriesTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `player records and ratings persist across repository instances`() {
        val database = SqliteDatabase(tempDir.resolve("registry.db"))
        val players = SqlitePlayerRepository(database)
        val first = players.saveAndReturn("p1", "Alice")
        first.updateRating(1240)
        players.save(first)
        players.saveAndReturn("p2", "Bob")

        val reloaded = SqlitePlayerRepository(SqliteDatabase(tempDir.resolve("registry.db")))

        assertEquals(listOf("p1", "p2"), reloaded.findAll().map { it.id })
        assertEquals(1240, reloaded.findById("p1")?.rating)
        assertEquals("Bob", reloaded.findById("p2")?.name)
        assertNull(reloaded.findById("missing"))
    }

    @Test
    fun `games moves boards ratings and statistics persist and restore after restart`() {
        val databasePath = tempDir.resolve("battleship.db")
        val validator = GameRulesValidatorImpl()
        val firstDatabase = SqliteDatabase(databasePath)
        val firstPlayers = SqlitePlayerRepository(firstDatabase)
        val firstGames = SqliteGameRepository(firstDatabase)
        val service = GameService(
            firstPlayers,
            firstGames,
            GameEngine(validator, EloRatingCalculator()),
            validator,
        )
        val first = service.addPlayer("Alice")
        val second = service.addPlayer("Bob")
        val session = service.startGame(
            first,
            second,
            BoardFactory.createValidClassicalBoard(),
            BoardFactory.createValidClassicalBoard(),
        )
        val firstShot = session.board2.ships.first().positions.first()

        assertEquals(MoveResult.HIT, service.recordMove(session, session.currentTurnPlayerId, firstShot))

        val restartedDatabase = SqliteDatabase(databasePath)
        val restartedPlayers = SqlitePlayerRepository(restartedDatabase)
        val restartedGames = SqliteGameRepository(restartedDatabase)
        val restored = requireNotNull(restartedGames.findById(session.id))
        assertEquals(GameState.IN_PROGRESS, restored.state)
        assertEquals(session.currentTurnPlayerId, restored.currentTurnPlayerId)
        assertEquals(session.moves, restored.moves)
        assertEquals(session.board2.shots, restored.board2.shots)
        assertEquals(session.board2.ships.map { it.hits }, restored.board2.ships.map { it.hits })
        assertEquals(listOf(1, 1), restartedGames.findPlayerStatistics().map { it.gamesPlayed })
        assertEquals(0, restartedGames.findPlayerStatistics().sumOf { it.wins + it.losses })

        val resumedService = GameService(
            restartedPlayers,
            restartedGames,
            GameEngine(validator, EloRatingCalculator()),
            validator,
        )
        val remainingTargetCells = restored.board2.ships.flatMap { it.positions }
            .filterNot { it in restored.board2.shots }
        remainingTargetCells.forEach { position ->
            resumedService.recordMove(restored, restored.currentTurnPlayerId, position)
        }

        val finalDatabase = SqliteDatabase(databasePath)
        val finalGames = SqliteGameRepository(finalDatabase)
        val persisted = requireNotNull(finalGames.findById(session.id))
        val statistics = finalGames.findPlayerStatistics().associateBy { it.playerId }
        assertEquals(GameState.FINISHED, persisted.state)
        assertEquals(first.id, persisted.winnerId)
        assertEquals(restored.moves, persisted.moves)
        assertEquals(1, statistics.getValue(first.id).gamesPlayed)
        assertEquals(1, statistics.getValue(first.id).wins)
        assertEquals(1, statistics.getValue(second.id).losses)
        assertEquals(1, statistics.getValue(second.id).gamesPlayed)

        finalGames.save(persisted)
        val savedStatistics = finalGames.findPlayerStatistics().associateBy { it.playerId }
        assertEquals(1, savedStatistics.getValue(first.id).wins)
        assertEquals(1, savedStatistics.getValue(second.id).losses)
    }

    private fun SqlitePlayerRepository.saveAndReturn(id: String, name: String) =
        battleship.core.Player(id, name).also(::save)
}
