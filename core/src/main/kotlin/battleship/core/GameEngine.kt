package battleship.core

class GameEngine(private val validator: IGameRulesValidator) {
    fun processMove(session: GameSession, playerId: String, pos: Position): MoveResult {
        if (!validator.validateMove(session, playerId, pos)) {
            return MoveResult.INVALID_MOVE
        }

        val targetBoard = if (playerId == session.player1.id) session.board2 else session.board1
        
        if (targetBoard.shots.contains(pos)) {
            return MoveResult.ALREADY_SHOT
        }

        targetBoard.addShot(pos)
        val hitShip = targetBoard.ships.find { pos in it.positions }

        return if (hitShip != null) {
            hitShip.addHit(pos)
            if (targetBoard.isAllShipsSunk()) {
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