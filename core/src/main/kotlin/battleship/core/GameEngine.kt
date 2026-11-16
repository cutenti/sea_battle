package battleship.core

interface IGameEngine {
    fun processMove(session: GameSession, playerId: String, pos: Position): MoveResult
}

/**
 * Правила одного выстрела. Движок не знает про репозиторий:
 * сохранить сессию после хода — обязанность [GameService].
 *
 * Идентификатор игрока передаётся явно. Иначе движок не отличит
 * выстрел активного игрока от чужого.
 */
class GameEngine(
    private val validator: IGameRulesValidator,
    private val ratingCalculator: IRatingCalculator = EloRatingCalculator(),
) : IGameEngine {
    override fun processMove(session: GameSession, playerId: String, pos: Position): MoveResult {
        if (!validator.validateMove(session, playerId, pos)) {
            return MoveResult.INVALID_MOVE
        }

        val targetBoard = if (playerId == session.player1.id) session.board2 else session.board1

        if (pos in targetBoard.shots) {
            return MoveResult.ALREADY_SHOT
        }

        targetBoard.addShot(pos)
        val hitShip = targetBoard.ships.find { pos in it.positions }

        if (hitShip == null) {
            session.switchTurn()
            return MoveResult.MISS
        }

        hitShip.addHit(pos)
        if (targetBoard.isAllShipsSunk()) {
            applyRating(session, playerId)
            session.finishGame(playerId)
            return MoveResult.GAME_OVER
        }
        return if (hitShip.isSunk()) MoveResult.KILLED else MoveResult.HIT
    }

    private fun applyRating(session: GameSession, winnerId: String) {
        val winner = if (winnerId == session.player1.id) session.player1 else session.player2
        val loser = if (winnerId == session.player1.id) session.player2 else session.player1
        val delta = ratingCalculator.calculateRatingChange(winner, loser)
        winner.updateRating(winner.rating + delta)
        // Пол нуля живёт в Player.updateRating. Здесь только не даём уйти ниже него.
        loser.updateRating((loser.rating - delta).coerceAtLeast(0))
    }
}
