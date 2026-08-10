package battleship.core

data class Position(val x: Int, val y: Int)

enum class MoveResult { MISS, HIT, KILLED, ALREADY_SHOT, INVALID_MOVE, GAME_OVER }
enum class GameState { SETUP, IN_PROGRESS, FINISHED }

class Player(val id: String, val name: String, rating: Int = 1000) {
    var rating: Int = rating
        private set

    fun updateRating(newRating: Int) {
        rating = newRating
    }
}

class Ship(val positions: List<Position>) {
    private val _hits = mutableSetOf<Position>()
    val hits: Set<Position> get() = _hits.toSet()

    fun addHit(pos: Position) {
        if (pos in positions) _hits.add(pos)
    }

    fun isSunk(): Boolean = _hits.size == positions.size
}

class Board(val ships: List<Ship>) {
    private val _shots = mutableSetOf<Position>()
    val shots: Set<Position> get() = _shots.toSet()

    fun addShot(pos: Position) {
        _shots.add(pos)
    }

    fun isAllShipsSunk(): Boolean = ships.all { it.isSunk() }
}

class GameSession(
    val id: String,
    val player1: Player,
    val player2: Player,
    val board1: Board,
    val board2: Board
) {
    var state: GameState = GameState.IN_PROGRESS
        private set
    var currentTurnPlayerId: String = player1.id
        private set
    var winnerId: String? = null
        private set

    fun switchTurn() {
        currentTurnPlayerId = if (currentTurnPlayerId == player1.id) player2.id else player1.id
    }

    fun finishGame(winner: String) {
        state = GameState.FINISHED
        winnerId = winner
    }
}