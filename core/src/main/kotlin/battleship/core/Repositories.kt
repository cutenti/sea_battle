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
}

/**
 * Хранит те же объекты, которые меняет движок.
 * Повторный save после хода нужен как контракт: когда на месте map появится база,
 * без явной записи диск не узнает об изменении.
 * LinkedHashMap сохраняет порядок добавления, поэтому список игроков стабилен.
 */
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
