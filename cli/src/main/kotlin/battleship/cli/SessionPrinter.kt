package battleship.cli

import battleship.core.Board
import battleship.core.GameSession
import battleship.core.GameState
import battleship.core.Player
import battleship.core.Position

/**
 * Администратор видит обе расстановки. Это журнал партии, а не клиент игрока,
 * поэтому корабли не скрываются.
 * x — столбец, y — строка, оба с нуля.
 */
object SessionPrinter {
    fun formatPlayers(players: List<Player>): String {
        if (players.isEmpty()) return Messages.NO_PLAYERS
        return players.joinToString("\n") { "${it.id}  ${it.name}  рейтинг ${it.rating}" }
    }

    fun format(session: GameSession): String = buildString {
        appendLine("Игра ${session.id}")
        appendLine("Состояние: ${session.state}")
        appendLine("Ход: ${session.currentTurnPlayerId}")
        session.winnerId?.let { winnerId ->
            val winner = playerById(session, winnerId)
            appendLine("Победитель: ${winner?.name ?: winnerId}")
        }
        appendLine(Messages.BOARD_LEGEND)
        appendLine("Поле ${session.player1.name}:")
        appendLine(formatBoard(session.board1))
        appendLine("Поле ${session.player2.name}:")
        appendLine(formatBoard(session.board2))
        appendLine("Журнал:")
        if (session.moves.isEmpty()) {
            append(Messages.NO_MOVES)
        } else {
            append(
                session.moves.mapIndexed { index, move ->
                    "${index + 1}. ${move.playerId} (${move.position.x}, ${move.position.y}) ${move.result}"
                }.joinToString("\n"),
            )
        }
    }

    fun formatBoard(board: Board): String {
        val sunk = board.ships.filter { it.isSunk() }.flatMap { it.positions }.toSet()
        val ships = board.ships.flatMap { it.positions }.toSet()
        val header = "  " + (0..9).joinToString(" ")
        val rows = (0..9).joinToString("\n") { y ->
            val cells = (0..9).joinToString(" ") { x ->
                mark(Position(x, y), ships, sunk, board)
            }
            "$y $cells"
        }
        return "$header\n$rows"
    }

    private fun mark(pos: Position, ships: Set<Position>, sunk: Set<Position>, board: Board): String {
        val shot = pos in board.shots
        return when {
            pos in sunk && shot -> "#"
            pos in ships && shot -> "X"
            shot -> "o"
            pos in ships -> "S"
            else -> "."
        }
    }

    private fun playerById(session: GameSession, id: String): Player? = when (id) {
        session.player1.id -> session.player1
        session.player2.id -> session.player2
        else -> null
    }

    fun finishedLine(session: GameSession): String? {
        if (session.state != GameState.FINISHED) return null
        val winner = playerById(session, session.winnerId ?: return null) ?: return null
        val loser = if (winner.id == session.player1.id) session.player2 else session.player1
        return Messages.GAME_FINISHED.format(
            winner.name,
            winner.name,
            winner.rating,
            loser.name,
            loser.rating,
        )
    }
}
