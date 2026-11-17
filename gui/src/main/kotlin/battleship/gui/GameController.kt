package battleship.gui

import battleship.core.Board
import battleship.core.GameSession
import battleship.core.BoardFactory
import battleship.core.GameService
import battleship.core.IGameRulesValidator
import battleship.core.MoveResult
import battleship.core.Player
import battleship.core.Position

data class PlayerStatistics(
    val player: Player,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
)

class GameController(
    private val service: GameService,
    private val validator: IGameRulesValidator,
    private val boardFactory: () -> Board = { BoardFactory.createRandomBoard(validator) },
    private val nanoTime: () -> Long = System::nanoTime,
) {
    var currentSession: GameSession? = null
        private set
    private var startedAtNanos: Long? = null
    private var finishedAtNanos: Long? = null

    fun players(): List<Player> = service.listPlayers()

    fun games(): List<GameSession> = service.listGames()

    fun addPlayer(name: String): Player = service.addPlayer(name.trim())

    fun startGame(firstPlayerId: String, secondPlayerId: String): GameSession {
        val first = service.findPlayer(firstPlayerId)
            ?: throw IllegalArgumentException("Первый игрок не найден")
        val second = service.findPlayer(secondPlayerId)
            ?: throw IllegalArgumentException("Второй игрок не найден")
        val session = service.startGame(first, second, boardFactory(), boardFactory())
        currentSession = session
        startedAtNanos = nanoTime()
        finishedAtNanos = null
        return session
    }

    fun shoot(position: Position): MoveResult {
        val session = currentSession ?: throw IllegalStateException("Сначала начните партию")
        val result = service.recordMove(session, session.currentTurnPlayerId, position)
        if (result == MoveResult.GAME_OVER) finishedAtNanos = nanoTime()
        return result
    }

    fun elapsedSeconds(): Long {
        val start = startedAtNanos ?: return 0
        val end = finishedAtNanos ?: nanoTime()
        return (end - start).coerceAtLeast(0) / 1_000_000_000L
    }

    fun statistics(): List<PlayerStatistics> {
        val statistics = service.listPlayerStatistics().associateBy { it.playerId }
        return service.listPlayers().map { player ->
            val saved = statistics[player.id]
            PlayerStatistics(
                player,
                gamesPlayed = saved?.gamesPlayed ?: 0,
                wins = saved?.wins ?: 0,
                losses = saved?.losses ?: 0,
            )
        }.sortedWith(compareByDescending<PlayerStatistics> { it.player.rating }.thenBy { it.player.name })
    }
}
