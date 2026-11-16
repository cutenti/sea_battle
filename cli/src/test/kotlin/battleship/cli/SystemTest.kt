package battleship.cli

import battleship.core.BoardFactory
import battleship.core.GameEngine
import battleship.core.GameRulesValidatorImpl
import battleship.core.GameService
import battleship.core.InMemoryGameRepository
import battleship.core.InMemoryPlayerRepository
import battleship.core.MoveResult
import battleship.core.Player
import battleship.core.Position
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SystemTest {

    @Test
    fun `service engine and repositories agree on a hit`() {
        val validator = GameRulesValidatorImpl()
        val games = InMemoryGameRepository()
        val service = GameService(
            InMemoryPlayerRepository(),
            games,
            GameEngine(validator),
            validator,
        )
        val first = Player("1", "P1")
        val second = Player("2", "P2")
        val board = BoardFactory.createValidClassicalBoard()
        assertTrue(validator.validateBoard(board))

        val session = service.startGame(first, second, board, board)
        val result = service.recordMove(session, "1", Position(0, 0))

        assertEquals(MoveResult.HIT, result)
        assertTrue(session.board2.shots.contains(Position(0, 0)))
        assertEquals(MoveResult.HIT, games.findById(session.id)?.moves?.single()?.result)
    }

    @Test
    fun `system test CLI loop input parsing and validation`() {
        val output = runMain(
            """
            unknown_cmd
            add_player
            move g1 p1 NaN 0
            exit
            """.trimIndent(),
        )

        assertTrue(output.contains(Messages.UNKNOWN_COMMAND))
        assertTrue(output.contains(Messages.INVALID_ARGUMENTS))
        assertTrue(output.contains(Messages.INVALID_COORDINATES))
        assertTrue(output.indexOf(Messages.PROMPT) < output.indexOf(Messages.UNKNOWN_COMMAND))
    }

    @Test
    fun `system test handles EOF gracefully without exit command`() {
        val output = runMain("add_player Alice\n")

        assertTrue(output.contains("Игрок Alice добавлен. ID:"))
    }

    @Test
    fun `cli can add players start a match and record a move`() {
        val output = runMain(
            """
            add_player Alice
            add_player Bob
            list_players
            start_game p1 p2
            move g1 p1 0 0
            show g1
            exit
            """.trimIndent(),
        )

        assertTrue(output.contains("Игрок Alice добавлен. ID: p1"))
        assertTrue(output.contains("Игрок Bob добавлен. ID: p2"))
        assertTrue(output.contains("Игра g1 началась"))
        assertTrue(output.contains("Результат: "))
        assertTrue(output.contains("Журнал:\n1. p1 (0, 0)"))
    }
}

private fun runMain(script: String): String {
    val inputStream = ByteArrayInputStream(script.toByteArray())
    val outputStream = ByteArrayOutputStream()
    val originalIn = System.`in`
    val originalOut = System.out
    return try {
        System.setIn(inputStream)
        System.setOut(PrintStream(outputStream))
        assertDoesNotThrow { main() }
        outputStream.toString("UTF-8")
    } finally {
        System.setIn(originalIn)
        System.setOut(originalOut)
    }
}
