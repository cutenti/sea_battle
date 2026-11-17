package battleship.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RepositoriesTest {

    @Test
    fun `InMemoryPlayerRepository saves and retrieves player correctly`() {
        val repo = InMemoryPlayerRepository()
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")

        repo.save(alice)
        repo.save(bob)

        assertEquals(alice, repo.findById("p1"))
        assertNull(repo.findById("missing"))
        assertEquals(listOf(alice, bob), repo.findAll())
    }

    @Test
    fun `InMemoryGameRepository saves and retrieves game session correctly`() {
        val repo = InMemoryGameRepository()
        val session = GameSession(
            "g1",
            Player("p1", "Alice"),
            Player("p2", "Bob"),
            Board(emptyList()),
            Board(emptyList()),
        )

        repo.save(session)

        assertEquals(session, repo.findById("g1"))
        assertNull(repo.findById("missing"))
        assertEquals(listOf(session), repo.findAll())
    }
}
