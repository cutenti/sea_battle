package battleship.cli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InputParserTest {

    @Test
    fun `parse returns Error when command has insufficient arguments`() {
        // Ошибка: add_player требует 1 аргумент (имя)
        val resultAdd = InputParser.parse("add_player")
        assertInstanceOf(ParsedCommand.Error::class.java, resultAdd)
        assertEquals(Messages.INVALID_ARGUMENTS, (resultAdd as ParsedCommand.Error).message)

        // Ошибка: start_game требует 2 аргумента (id1, id2)
        val resultStart = InputParser.parse("start_game p1")
        assertInstanceOf(ParsedCommand.Error::class.java, resultStart)
        assertEquals(Messages.INVALID_ARGUMENTS, (resultStart as ParsedCommand.Error).message)

        // Ошибка: move требует 4 аргумента (game_id, player_id, x, y)
        val resultMove = InputParser.parse("move g1 p1 0")
        assertInstanceOf(ParsedCommand.Error::class.java, resultMove)
        assertEquals(Messages.INVALID_ARGUMENTS, (resultMove as ParsedCommand.Error).message)
    }

    @Test
    fun `parse returns Error when coordinates are not integers`() {
        // Координата X — строка "abc"
        val resultNotIntX = InputParser.parse("move g1 p1 abc 5")
        assertInstanceOf(ParsedCommand.Error::class.java, resultNotIntX)
        assertEquals(Messages.INVALID_COORDINATES, (resultNotIntX as ParsedCommand.Error).message)

        // Координата Y — дробное число "1.5"
        val resultNotIntY = InputParser.parse("move g1 p1 0 1.5")
        assertInstanceOf(ParsedCommand.Error::class.java, resultNotIntY)
        assertEquals(Messages.INVALID_COORDINATES, (resultNotIntY as ParsedCommand.Error).message)
    }

    @Test
    fun `parse returns Error on unknown command`() {
        val result = InputParser.parse("invalid_command_name")
        assertInstanceOf(ParsedCommand.Error::class.java, result)
        assertEquals(Messages.UNKNOWN_COMMAND, (result as ParsedCommand.Error).message)
    }

    @Test
    fun `parse correctly parses valid move command`() {
        val result = InputParser.parse("move g1 p1 3 4")
        assertInstanceOf(ParsedCommand.Move::class.java, result)
        val move = result as ParsedCommand.Move
        assertEquals("g1", move.gameId)
        assertEquals("p1", move.playerId)
        assertEquals(3, move.x)
        assertEquals(4, move.y)
    }
}