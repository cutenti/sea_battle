package battleship.persistence

import battleship.core.Board
import battleship.core.GameSession
import battleship.core.GameState
import battleship.core.IGameRepository
import battleship.core.MoveRecord
import battleship.core.MoveResult
import battleship.core.Player
import battleship.core.PlayerGameStatistics
import battleship.core.Position
import battleship.core.Ship
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class SqliteGameRepository(private val database: SqliteDatabase) : IGameRepository {
    override fun save(session: GameSession) {
        database.transaction { connection ->
            upsertPlayer(connection, session.player1)
            upsertPlayer(connection, session.player2)
            val previousState = connection.prepareStatement("SELECT state FROM games WHERE id = ?").use { statement ->
                statement.setString(1, session.id)
                statement.executeQuery().use { result -> if (result.next()) result.getString("state") else null }
            }

            connection.prepareStatement(
                """
                INSERT INTO games(id, player1_id, player2_id, state, current_turn_player_id, winner_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    player1_id = excluded.player1_id,
                    player2_id = excluded.player2_id,
                    state = excluded.state,
                    current_turn_player_id = excluded.current_turn_player_id,
                    winner_id = excluded.winner_id
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, session.id)
                statement.setString(2, session.player1.id)
                statement.setString(3, session.player2.id)
                statement.setString(4, session.state.name)
                statement.setString(5, session.currentTurnPlayerId)
                statement.setString(6, session.winnerId)
                statement.executeUpdate()
            }

            when {
                previousState == null -> {
                    if (session.state != GameState.SETUP) updateGameCount(connection, session)
                    if (session.state == GameState.FINISHED) updateResultCount(connection, session)
                }
                previousState != GameState.FINISHED.name && session.state == GameState.FINISHED ->
                    updateResultCount(connection, session)
            }

            connection.prepareStatement("DELETE FROM boards WHERE game_id = ?").use { statement ->
                statement.setString(1, session.id)
                statement.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM moves WHERE game_id = ?").use { statement ->
                statement.setString(1, session.id)
                statement.executeUpdate()
            }
            writeBoard(connection, session.id, 1, session.board1)
            writeBoard(connection, session.id, 2, session.board2)
            writeMoves(connection, session)
        }
    }

    override fun findById(id: String): GameSession? = database.openConnection().use { connection ->
        readGame(connection, id)
    }

    override fun findAll(): List<GameSession> = database.openConnection().use { connection ->
        val ids = connection.createStatement().use { statement ->
            statement.executeQuery("SELECT id FROM games ORDER BY rowid").use { result ->
                buildList {
                    while (result.next()) add(result.getString("id"))
                }
            }
        }
        ids.map { id -> readGame(connection, id) ?: throw SQLException("Не удалось прочитать партию $id") }
    }

    override fun findPlayerStatistics(): List<PlayerGameStatistics> = database.openConnection().use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT player_id, games_played, wins, losses FROM player_statistics ORDER BY rowid",
            ).use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            PlayerGameStatistics(
                                playerId = result.getString("player_id"),
                                gamesPlayed = result.getInt("games_played"),
                                wins = result.getInt("wins"),
                                losses = result.getInt("losses"),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun readGame(connection: Connection, id: String): GameSession? {
        val metadata = connection.prepareStatement(
            """
            SELECT id, player1_id, player2_id, state, current_turn_player_id, winner_id
            FROM games WHERE id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { result -> if (result.next()) GameMetadata.from(result) else null }
        } ?: return null

        val players = connection.prepareStatement("SELECT id, name, rating FROM players WHERE id IN (?, ?)").use { statement ->
            statement.setString(1, metadata.player1Id)
            statement.setString(2, metadata.player2Id)
            statement.executeQuery().use { result ->
                buildMap {
                    while (result.next()) {
                        val player = Player(result.getString("id"), result.getString("name"), result.getInt("rating"))
                        put(player.id, player)
                    }
                }
            }
        }
        val player1 = players[metadata.player1Id] ?: throw SQLException("Игрок ${metadata.player1Id} не найден")
        val player2 = players[metadata.player2Id] ?: throw SQLException("Игрок ${metadata.player2Id} не найден")
        val boards = (1..2).map { boardNumber -> readBoard(connection, id, boardNumber) }
        val moves = readMoves(connection, id)

        return GameSession.restore(
            id = metadata.id,
            player1 = player1,
            player2 = player2,
            board1 = boards[0],
            board2 = boards[1],
            state = metadata.state,
            currentTurnPlayerId = metadata.currentTurnPlayerId,
            winnerId = metadata.winnerId,
            moves = moves,
        )
    }

    private fun readBoard(connection: Connection, gameId: String, boardNumber: Int): Board {
        val exists = connection.prepareStatement(
            "SELECT 1 FROM boards WHERE game_id = ? AND board_number = ?",
        ).use { statement ->
            statement.setString(1, gameId)
            statement.setInt(2, boardNumber)
            statement.executeQuery().use(ResultSet::next)
        }
        if (!exists) throw SQLException("В партии $gameId отсутствует поле $boardNumber")

        val ships = connection.prepareStatement(
            "SELECT positions, hits FROM ships WHERE game_id = ? AND board_number = ? ORDER BY ship_number",
        ).use { statement ->
            statement.setString(1, gameId)
            statement.setInt(2, boardNumber)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val positions = decodePositions(result.getString("positions"))
                        val hits = decodePositions(result.getString("hits")).toSet()
                        add(Ship(positions, hits))
                    }
                }
            }
        }
        val shots = connection.prepareStatement(
            "SELECT x, y FROM shots WHERE game_id = ? AND board_number = ?",
        ).use { statement ->
            statement.setString(1, gameId)
            statement.setInt(2, boardNumber)
            statement.executeQuery().use { result ->
                buildSet {
                    while (result.next()) add(Position(result.getInt("x"), result.getInt("y")))
                }
            }
        }
        return Board(ships, shots)
    }

    private fun readMoves(connection: Connection, gameId: String): List<MoveRecord> {
        return connection.prepareStatement(
            "SELECT player_id, x, y, result FROM moves WHERE game_id = ? ORDER BY move_number",
        ).use { statement ->
            statement.setString(1, gameId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            MoveRecord(
                                playerId = result.getString("player_id"),
                                position = Position(result.getInt("x"), result.getInt("y")),
                                result = parseEnum(result.getString("result"), "результат хода", MoveResult.entries.toTypedArray()),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun writeBoard(connection: Connection, gameId: String, boardNumber: Int, board: Board) {
        connection.prepareStatement("INSERT INTO boards(game_id, board_number) VALUES (?, ?)").use { statement ->
            statement.setString(1, gameId)
            statement.setInt(2, boardNumber)
            statement.executeUpdate()
        }
        connection.prepareStatement(
            "INSERT INTO ships(game_id, board_number, ship_number, positions, hits) VALUES (?, ?, ?, ?, ?)",
        ).use { statement ->
            board.ships.forEachIndexed { index, ship ->
                statement.setString(1, gameId)
                statement.setInt(2, boardNumber)
                statement.setInt(3, index)
                statement.setString(4, encodePositions(ship.positions))
                statement.setString(5, encodePositions(ship.hits))
                statement.addBatch()
            }
            statement.executeBatch()
        }
        connection.prepareStatement("INSERT INTO shots(game_id, board_number, x, y) VALUES (?, ?, ?, ?)").use { statement ->
            board.shots.forEach { position ->
                statement.setString(1, gameId)
                statement.setInt(2, boardNumber)
                statement.setInt(3, position.x)
                statement.setInt(4, position.y)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun writeMoves(connection: Connection, session: GameSession) {
        connection.prepareStatement(
            """
            INSERT INTO moves(game_id, move_number, player_id, x, y, result)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            session.moves.forEachIndexed { index, move ->
                statement.setString(1, session.id)
                statement.setInt(2, index)
                statement.setString(3, move.playerId)
                statement.setInt(4, move.position.x)
                statement.setInt(5, move.position.y)
                statement.setString(6, move.result.name)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun updateGameCount(connection: Connection, session: GameSession) {
        connection.prepareStatement(
            "UPDATE player_statistics SET games_played = games_played + 1 WHERE player_id IN (?, ?)",
        ).use { statement ->
            statement.setString(1, session.player1.id)
            statement.setString(2, session.player2.id)
            statement.executeUpdate()
        }
    }

    private fun updateResultCount(connection: Connection, session: GameSession) {
        val winnerId = session.winnerId ?: throw SQLException("Завершённая партия должна иметь победителя")
        connection.prepareStatement(
            """
            UPDATE player_statistics
            SET wins = wins + CASE WHEN player_id = ? THEN 1 ELSE 0 END,
                losses = losses + CASE WHEN player_id = ? THEN 0 ELSE 1 END
            WHERE player_id IN (?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, winnerId)
            statement.setString(2, winnerId)
            statement.setString(3, session.player1.id)
            statement.setString(4, session.player2.id)
            statement.executeUpdate()
        }
    }

    private fun encodePositions(positions: Iterable<Position>): String =
        positions.joinToString(";") { "${it.x}:${it.y}" }

    private fun decodePositions(encoded: String): List<Position> {
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(';').map { item ->
            val coordinates = item.split(':')
            if (coordinates.size != 2) throw SQLException("Некорректные координаты в базе партий")
            val x = coordinates[0].toIntOrNull() ?: throw SQLException("Некорректная координата x в базе партий")
            val y = coordinates[1].toIntOrNull() ?: throw SQLException("Некорректная координата y в базе партий")
            Position(x, y)
        }.also { positions ->
            if (positions.distinct().size != positions.size) {
                throw SQLException("Повторяющиеся координаты в базе партий")
            }
        }
    }

    private data class GameMetadata(
        val id: String,
        val player1Id: String,
        val player2Id: String,
        val state: GameState,
        val currentTurnPlayerId: String,
        val winnerId: String?,
    ) {
        companion object {
            fun from(result: ResultSet): GameMetadata = GameMetadata(
                id = result.getString("id"),
                player1Id = result.getString("player1_id"),
                player2Id = result.getString("player2_id"),
                state = parseEnum(result.getString("state"), "состояние партии", GameState.entries.toTypedArray()),
                currentTurnPlayerId = result.getString("current_turn_player_id"),
                winnerId = result.getString("winner_id"),
            )
        }
    }

    private companion object {
        fun <T : Enum<T>> parseEnum(value: String, field: String, values: Array<T>): T =
            values.firstOrNull { it.name == value }
                ?: throw SQLException("Некорректное значение $field в базе партий: $value")
    }
}
