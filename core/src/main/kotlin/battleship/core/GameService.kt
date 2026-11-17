package battleship.core

/**
 * Сценарии администрирования партии.
 * Консоль разбирает строку и печатает ответ. Проверки и запись делает сервис.
 */
class GameService(
    private val players: IPlayerRepository,
    private val games: IGameRepository,
    private val engine: IGameEngine,
    private val validator: IGameRulesValidator,
) {
    fun addPlayer(name: String): Player {
        val player = Player("p${players.findAll().size + 1}", name)
        players.save(player)
        return player
    }

    fun findPlayer(id: String): Player? = players.findById(id)

    fun listPlayers(): List<Player> = players.findAll()

    fun findGame(id: String): GameSession? = games.findById(id)

    fun listGames(): List<GameSession> = games.findAll()

    /**
     * Партия создаётся в [GameState.SETUP] и переводится в [GameState.IN_PROGRESS]
     * только после проверки обеих расстановок.
     */
    fun startGame(player1: Player, player2: Player, board1: Board, board2: Board): GameSession {
        if (player1.id == player2.id) {
            throw InvalidGameSetupException("Нужны два разных игрока.")
        }
        if (!validator.validateBoard(board1) || !validator.validateBoard(board2)) {
            throw InvalidGameSetupException("Расстановка кораблей нарушает правила.")
        }
        val session = GameSession(
            id = "g${games.findAll().size + 1}",
            player1 = player1,
            player2 = player2,
            board1 = copyLayout(board1),
            board2 = copyLayout(board2),
        )
        session.begin()
        games.save(session)
        return session
    }

    /**
     * Журнал получает каждый результат, включая отклонённый ход.
     * Рейтинг игроков переписывается в хранилище только при завершении партии:
     * до этого очки не меняются.
     */
    fun recordMove(session: GameSession, playerId: String, pos: Position): MoveResult {
        val result = engine.processMove(session, playerId, pos)
        session.addMove(MoveRecord(playerId, pos, result))
        games.save(session)
        if (result == MoveResult.GAME_OVER) {
            players.save(session.player1)
            players.save(session.player2)
        }
        return result
    }

    /**
     * Новые корабли и пустые попадания. Общий экземпляр доски у двух игроков
     * не приведёт к тому, что выстрел по одному полю изменит другое.
     */
    private fun copyLayout(board: Board): Board {
        return Board(board.ships.map { ship -> Ship(ship.positions.toList()) })
    }
}

class InvalidGameSetupException(message: String) : IllegalArgumentException(message)
