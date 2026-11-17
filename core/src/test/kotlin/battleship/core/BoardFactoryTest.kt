package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BoardFactoryTest {
    private val validator = GameRulesValidatorImpl()

    @Test
    fun `random board is legal and the same seed repeats the layout`() {
        val first = BoardFactory.createRandomBoard(validator, Random(1))
        val second = BoardFactory.createRandomBoard(validator, Random(1))

        assertTrue(validator.validateBoard(first))
        assertEquals(first.ships.map { it.positions }, second.ships.map { it.positions })
    }
}
