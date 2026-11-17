package battleship.core

interface IPlayerRepository {
    fun save(player: Player)
    fun findById(id: String): Player?
}

interface IGameRepository {
    fun save(session: GameSession)
    fun findById(id: String): GameSession?
}

class InMemoryPlayerRepository : IPlayerRepository {
    private val players = mutableMapOf<String, Player>()

    override fun save(player: Player) {
        players[player.id] = player
    }

    override fun findById(id: String): Player? = players[id]
}

class InMemoryGameRepository : IGameRepository {
    private val games = mutableMapOf<String, GameSession>()

    override fun save(session: GameSession) {
        games[session.id] = session
    }

    override fun findById(id: String): GameSession? = games[id]
}