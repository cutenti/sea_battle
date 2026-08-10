class InMemoryPlayerRepository {
    private val players = mutableMapOf<String, Player>()
    fun save(player: Player) { players[player.id] = player }
    fun findById(id: String): Player? = players[id]
}

class InMemoryGameRepository {
    private val games = mutableMapOf<String, GameSession>()
    fun save(session: GameSession) { games[session.id] = session }
    fun findById(id: String): GameSession? = games[id]
}