interface IGameRulesValidator {
    fun isValidPosition(pos: Position): Boolean
    fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean
}

class GameRulesValidatorImpl : IGameRulesValidator {
    override fun isValidPosition(pos: Position): Boolean =
        pos.x in 0..9 && pos.y in 0..9

    override fun validateMove(session: GameSession, playerId: String, pos: Position): Boolean {
        if (session.state != GameState.IN_PROGRESS) return false
        if (session.currentTurnPlayerId != playerId) return false
        return isValidPosition(pos)
    }
}

class GameEngine(private val validator: IGameRulesValidator) {

    fun processMove(session: GameSession, playerId: String, pos: Position): MoveResult {
        if (!validator.validateMove(session, playerId, pos)) {
            return MoveResult.INVALID_MOVE
        }

        val targetBoard = if (playerId == session.player1.id) session.board2 else session.board1
        
        if (targetBoard.shots.contains(pos)) {
            return MoveResult.ALREADY_SHOT
        }

        targetBoard.shots.add(pos)
        val hitShip = targetBoard.ships.find { pos in it.positions }

        return if (hitShip != null) {
            hitShip.hits.add(pos)
            if (targetBoard.isAllShipsSunk()) {
                session.state = GameState.FINISHED
                session.winnerId = playerId
                MoveResult.GAME_OVER
            } else if (hitShip.isSunk()) {
                MoveResult.KILLED
            } else {
                MoveResult.HIT
            }
        } else {
            // Смена хода при промахе
            session.currentTurnPlayerId = if (playerId == session.player1.id) session.player2.id else session.player1.id
            MoveResult.MISS
        }
    }
}