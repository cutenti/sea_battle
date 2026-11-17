package battleship.core

interface IPlayerRepository {
    fun save(player: Player)
    fun findById(id: String): Player?
    fun findAll(): List<Player>
}

interface IGameRepository {
    fun save(session: GameSession)
    fun findById(id: String): GameSession?
    fun findAll(): List<GameSession>

    fun findPlayerStatistics(): List<PlayerGameStatistics> {
        val sessions = findAll()
        return sessions
            .flatMap { session -> listOf(session.player1.id, session.player2.id) }
            .distinct()
            .map { playerId ->
                val playerGames = sessions.filter {
                    it.player1.id == playerId || it.player2.id == playerId
                }
                PlayerGameStatistics(
                    playerId = playerId,
                    gamesPlayed = playerGames.size,
                    wins = playerGames.count { it.state == GameState.FINISHED && it.winnerId == playerId },
                    losses = playerGames.count { it.state == GameState.FINISHED && it.winnerId != playerId },
                )
            }
    }
}

data class PlayerGameStatistics(
    val playerId: String,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
)

/** In-memory adapters preserve insertion order and store the mutable entities by reference. */
class InMemoryPlayerRepository : IPlayerRepository {
    private val players = linkedMapOf<String, Player>()

    override fun save(player: Player) {
        players[player.id] = player
    }

    override fun findById(id: String): Player? = players[id]

    override fun findAll(): List<Player> = players.values.toList()
}

class InMemoryGameRepository : IGameRepository {
    private val games = linkedMapOf<String, GameSession>()

    override fun save(session: GameSession) {
        games[session.id] = session
    }

    override fun findById(id: String): GameSession? = games[id]

    override fun findAll(): List<GameSession> = games.values.toList()
}
