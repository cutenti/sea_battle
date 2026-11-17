package battleship.core

data class Position(val x: Int, val y: Int)

enum class MoveResult { MISS, HIT, KILLED, ALREADY_SHOT, INVALID_MOVE, GAME_OVER }
enum class GameState { SETUP, IN_PROGRESS, FINISHED }
data class MoveRecord(val playerId: String, val position: Position, val result: MoveResult)

/** Mutable player identity whose rating is updated after a game. */
class Player(val id: String, val name: String, rating: Int = 1000) {
    var rating: Int = rating
        private set

    init {
        require(rating >= 0) { "Рейтинг не может быть отрицательным" }
    }

    fun updateRating(newRating: Int) {
        require(newRating >= 0) { "Рейтинг не может быть отрицательным" }
        rating = newRating
    }
}

class Ship(val positions: List<Position>, hits: Set<Position> = emptySet()) {
    private val _hits = mutableSetOf<Position>()
    val hits: Set<Position> get() = _hits.toSet()

    init {
        require(hits.all { it in positions }) { "Попадания должны находиться на корабле" }
        _hits.addAll(hits)
    }

    fun addHit(pos: Position) {
        if (pos in positions) _hits.add(pos)
    }

    fun isSunk(): Boolean = positions.isNotEmpty() && _hits.size == positions.size
}

class Board(ships: List<Ship>, shots: Set<Position> = emptySet()) {
    val ships: List<Ship> = ships.toList()
    private val _shots = mutableSetOf<Position>()
    val shots: Set<Position> get() = _shots.toSet()

    init {
        _shots.addAll(shots)
    }

    fun addShot(pos: Position) {
        _shots.add(pos)
    }

    fun isAllShipsSunk(): Boolean = ships.isNotEmpty() && ships.all { it.isSunk() }
}

class GameSession(
    val id: String,
    val player1: Player,
    val player2: Player,
    val board1: Board,
    val board2: Board,
) {
    var state: GameState = GameState.SETUP
        private set
    var currentTurnPlayerId: String = player1.id
        private set
    var winnerId: String? = null
        private set
    private val _moves = mutableListOf<MoveRecord>()
    val moves: List<MoveRecord> get() = _moves.toList()

    internal fun begin() {
        check(state == GameState.SETUP) { "Партию можно начать только из состояния SETUP" }
        state = GameState.IN_PROGRESS
    }

    fun switchTurn() {
        check(state == GameState.IN_PROGRESS) { "Передавать ход можно только в идущей партии" }
        currentTurnPlayerId = if (currentTurnPlayerId == player1.id) player2.id else player1.id
    }

    fun finishGame(winner: String) {
        check(state == GameState.IN_PROGRESS) { "Завершить можно только в идущей партии" }
        state = GameState.FINISHED
        winnerId = winner
    }

    fun addMove(record: MoveRecord) {
        _moves.add(record)
    }

    companion object {
        fun restore(
            id: String,
            player1: Player,
            player2: Player,
            board1: Board,
            board2: Board,
            state: GameState,
            currentTurnPlayerId: String,
            winnerId: String?,
            moves: List<MoveRecord>,
        ): GameSession {
            require(player1.id != player2.id) { "Игроки партии должны различаться" }
            require(currentTurnPlayerId == player1.id || currentTurnPlayerId == player2.id) {
                "Текущий игрок должен участвовать в партии"
            }
            require(winnerId == null || winnerId == player1.id || winnerId == player2.id) {
                "Победитель должен участвовать в партии"
            }
            require((state == GameState.FINISHED) == (winnerId != null)) {
                "Завершённая партия должна иметь победителя"
            }

            return GameSession(id, player1, player2, board1, board2).also { session ->
                session.state = state
                session.currentTurnPlayerId = currentTurnPlayerId
                session.winnerId = winnerId
                session._moves.addAll(moves)
            }
        }
    }
}
