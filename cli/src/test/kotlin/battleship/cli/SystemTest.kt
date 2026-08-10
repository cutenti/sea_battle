package battleship.cli

import battleship.core.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SystemTest {

    @Test
    fun `integration test between Engine, Validator and Repositories`() {
        val playerRepo = InMemoryPlayerRepository()
        val gameRepo = InMemoryGameRepository()
        val validator = GameRulesValidatorImpl()
        val engine = GameEngine(validator)

        val p1 = Player("1", "P1")
        val p2 = Player("2", "P2")
        playerRepo.save(p1)
        playerRepo.save(p2)

        val b1 = BoardFactory.createValidClassicalBoard()
        val b2 = BoardFactory.createValidClassicalBoard()
        assertTrue(validator.validateBoard(b1))

        val session = GameSession("g1", p1, p2, b1, b2)
        gameRepo.save(session)

        val retrievedSession = gameRepo.findById("g1")
        assertNotNull(retrievedSession)

        val result = engine.processMove(retrievedSession!!, "1", Position(0, 0))
        assertEquals(MoveResult.HIT, result)
        assertTrue(retrievedSession.board2.shots.contains(Position(0, 0)))
    }

    @Test
    fun `system test CLI loop input parsing and validation`() {
        val simulatedInput = buildString {
            appendLine("unknown_cmd")
            appendLine("add_player") // Ошибка числа аргументов
            appendLine("move g1 p1 NaN 0") // Ошибка парсинга координат
            appendLine("exit")
        }

        val inputStream = ByteArrayInputStream(simulatedInput.toByteArray())
        val outputStream = ByteArrayOutputStream()

        val originalIn = System.`in`
        val originalOut = System.out

        try {
            System.setIn(inputStream)
            System.setOut(PrintStream(outputStream))

            main()

            val output = outputStream.toString("UTF-8")
            
            assertTrue(output.contains(Messages.UNKNOWN_COMMAND))
            assertTrue(output.contains(Messages.INVALID_ARGUMENTS))
            assertTrue(output.contains(Messages.INVALID_COORDINATES))
        } finally {
            System.setIn(originalIn)
            System.setOut(originalOut)
        }
    }

    @Test
    fun `system test handles EOF gracefully without exit command`() {
        // Поток ввода заканчивается сразу после добавления игрока, команды exit нет
        val simulatedInput = "add_player Alice\n"
        
        val inputStream = ByteArrayInputStream(simulatedInput.toByteArray())
        val outputStream = ByteArrayOutputStream()

        val originalIn = System.`in`
        val originalOut = System.out

        try {
            System.setIn(inputStream)
            System.setOut(PrintStream(outputStream))

            // Вызов main() не должен выбрасывать NoSuchElementException
            assertDoesNotThrow { main() }

            val output = outputStream.toString("UTF-8")
            assertTrue(output.contains(Messages.PLAYER_ADDED.format("Alice", "").split("ID:")[0]))
        } finally {
            System.setIn(originalIn)
            System.setOut(originalOut)
        }
    }
}