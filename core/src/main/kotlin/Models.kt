data class Position(val x: Int, val y: Int)

enum class MoveResult { MISS, HIT, KILLED, ALREADY_SHOT, INVALID_MOVE, GAME_OVER }
enum class GameState { SETUP, IN_PROGRESS, FINISHED }

class Player(val id: String, val name: String, var rating: Int = 1000)

class Ship(val positions: List<Position>) {
    val hits = mutableSetOf<Position>()
    fun isSunk() = hits.size == positions.size
}

class Board(val ships: List<Ship>) {
    val shots = mutableSetOf<Position>()

    fun isAllShipsSunk() = ships.all { it.isSunk() }
}

class GameSession(
    val id: String,
    val player1: Player,
    val player2: Player,
    val board1: Board,
    val board2: Board
) {
    var state: GameState = GameState.IN_PROGRESS
    var currentTurnPlayerId: String = player1.id
    var winnerId: String? = null
}