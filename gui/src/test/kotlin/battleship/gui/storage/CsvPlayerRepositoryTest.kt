package battleship.gui.storage

import battleship.core.Player
import java.io.IOException
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CsvPlayerRepositoryTest {
    @TempDir
    lateinit var tempDir: java.nio.file.Path

    @Test
    fun `repository persists players and escaped CSV values across instances`() {
        val file = tempDir.resolve("registry/players.csv")
        val repository = CsvPlayerRepository(file)
        val player = Player("p1", "Alice, \"Ace\"")
        player.updateRating(1240)
        repository.save(player)
        repository.save(Player("p2", "Bob"))

        val reloaded = CsvPlayerRepository(file)

        assertEquals(listOf("p1", "p2"), reloaded.findAll().map { it.id })
        assertEquals("Alice, \"Ace\"", reloaded.findById("p1")?.name)
        assertEquals(1240, reloaded.findById("p1")?.rating)
        assertTrue(Files.readString(file).contains("\"Alice, \"\"Ace\"\"\""))
    }

    @Test
    fun `repository returns null for an unknown player`() {
        val repository = CsvPlayerRepository(tempDir.resolve("players.csv"))

        assertNull(repository.findById("missing"))
    }

    @Test
    fun `repository rejects malformed registry without silently accepting it`() {
        val file = tempDir.resolve("players.csv")
        Files.writeString(file, "id,name,rating\np1,Alice,not-a-number\n")

        assertThrows(IOException::class.java) { CsvPlayerRepository(file) }
    }
}
