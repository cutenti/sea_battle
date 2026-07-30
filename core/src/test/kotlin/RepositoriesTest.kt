import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RepositoriesTest {

    @Test
    fun `InMemoryPlayerRepository saves and retrieves player correctly`() {
        val repo = InMemoryPlayerRepository()
        val player = Player("p1", "Alice")
        
        repo.save(player)
        
        assertEquals(player, repo.findById("p1"))
        assertNull(repo.findById("non-existent-id"))
    }

    @Test
    fun `InMemoryGameRepository saves and retrieves game session correctly`() {
        val repo = InMemoryGameRepository()
        val p1 = Player("p1", "Alice")
        val p2 = Player("p2", "Bob")
        val session = GameSession("g1", p1, p2, Board(emptyList()), Board(emptyList()))
        
        repo.save(session)
        
        assertEquals(session, repo.findById("g1"))
        assertNull(repo.findById("non-existent-id"))
    }
}