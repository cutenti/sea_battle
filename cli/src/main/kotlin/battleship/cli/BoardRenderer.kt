package battleship.cli

import battleship.core.Board
import battleship.core.GameSession
import battleship.core.Position

object BoardRenderer {
    fun renderForPlayer(session: GameSession, playerId: String): String {
        val ownBoard: Board
        val opponentBoard: Board
        when (playerId) {
            session.player1.id -> {
                ownBoard = session.board1
                opponentBoard = session.board2
            }
            session.player2.id -> {
                ownBoard = session.board2
                opponentBoard = session.board1
            }
            else -> throw IllegalArgumentException("Player is not part of this game")
        }

        return buildString {
            appendLine("Ваше поле:")
            appendLine(renderBoard(ownBoard, revealShips = true))
            appendLine("Поле противника:")
            append(renderBoard(opponentBoard, revealShips = false))
        }
    }

    private fun renderBoard(board: Board, revealShips: Boolean): String = buildString {
        appendLine("  0 1 2 3 4 5 6 7 8 9")
        for (y in 0..9) {
            append("$y ")
            for (x in 0..9) {
                val position = Position(x, y)
                val ship = board.ships.find { position in it.positions }
                val cell = when {
                    position in board.shots && ship != null -> "X"
                    position in board.shots -> "o"
                    ship != null && revealShips -> "S"
                    else -> "."
                }
                append(cell)
                if (x < 9) append(' ')
            }
            if (y < 9) appendLine()
        }
    }
}
