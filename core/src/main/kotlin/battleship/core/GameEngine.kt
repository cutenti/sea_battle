package battleship.core

class GameEngine(
    private val validator: IGameRulesValidator,
    private val ratingCalculator: IRatingCalculator = FixedRatingCalculator()
) {
    fun startGame(session: GameSession): Boolean {
        if (session.state != GameState.SETUP) return false
        if (!validator.validateBoard(session.board1) || !validator.validateBoard(session.board2)) return false

        session.begin()
        return true
    }

    fun processMove(session: GameSession, playerId: String, pos: Position): MoveResult {
        val result = if (!validator.validateMove(session, playerId, pos)) {
            MoveResult.INVALID_MOVE
        } else {
            val targetBoard = if (playerId == session.player1.id) session.board2 else session.board1

            if (targetBoard.shots.contains(pos)) {
                MoveResult.ALREADY_SHOT
            } else {
                targetBoard.addShot(pos)
                val hitShip = targetBoard.ships.find { pos in it.positions }

                if (hitShip != null) {
                    hitShip.addHit(pos)
                    if (targetBoard.isAllShipsSunk()) {
                        val winner = if (playerId == session.player1.id) session.player1 else session.player2
                        val loser = if (playerId == session.player1.id) session.player2 else session.player1
                        val ratingChange = ratingCalculator.calculateRatingChange(winner, loser)
                        winner.updateRating(winner.rating + ratingChange)
                        loser.updateRating(loser.rating - ratingChange)
                        session.finishGame(playerId)
                        MoveResult.GAME_OVER
                    } else if (hitShip.isSunk()) {
                        MoveResult.KILLED
                    } else {
                        MoveResult.HIT
                    }
                } else {
                    session.switchTurn()
                    MoveResult.MISS
                }
            }
        }

        session.recordMove(MoveRecord(playerId, pos, result))
        return result
    }
}
