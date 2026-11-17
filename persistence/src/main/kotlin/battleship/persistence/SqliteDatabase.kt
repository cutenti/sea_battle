package battleship.persistence

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class SqliteDatabase(path: Path) {
    private val file = path.toAbsolutePath()
    private val jdbcUrl: String

    init {
        val parent = file.parent ?: throw IllegalArgumentException("Путь к базе должен иметь родительский каталог")
        Files.createDirectories(parent)
        jdbcUrl = "jdbc:sqlite:$file"
        openConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS players (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        rating INTEGER NOT NULL CHECK (rating >= 0)
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS player_statistics (
                        player_id TEXT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
                        games_played INTEGER NOT NULL DEFAULT 0 CHECK (games_played >= 0),
                        wins INTEGER NOT NULL DEFAULT 0 CHECK (wins >= 0),
                        losses INTEGER NOT NULL DEFAULT 0 CHECK (losses >= 0)
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS games (
                        id TEXT PRIMARY KEY,
                        player1_id TEXT NOT NULL REFERENCES players(id),
                        player2_id TEXT NOT NULL REFERENCES players(id),
                        state TEXT NOT NULL,
                        current_turn_player_id TEXT NOT NULL,
                        winner_id TEXT REFERENCES players(id),
                        CHECK (player1_id <> player2_id)
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS boards (
                        game_id TEXT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                        board_number INTEGER NOT NULL CHECK (board_number IN (1, 2)),
                        PRIMARY KEY (game_id, board_number)
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS ships (
                        game_id TEXT NOT NULL,
                        board_number INTEGER NOT NULL,
                        ship_number INTEGER NOT NULL,
                        positions TEXT NOT NULL,
                        hits TEXT NOT NULL,
                        PRIMARY KEY (game_id, board_number, ship_number),
                        FOREIGN KEY (game_id, board_number)
                            REFERENCES boards(game_id, board_number) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS shots (
                        game_id TEXT NOT NULL,
                        board_number INTEGER NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        PRIMARY KEY (game_id, board_number, x, y),
                        FOREIGN KEY (game_id, board_number)
                            REFERENCES boards(game_id, board_number) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS moves (
                        game_id TEXT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                        move_number INTEGER NOT NULL,
                        player_id TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        result TEXT NOT NULL,
                        PRIMARY KEY (game_id, move_number)
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    internal fun openConnection(): Connection =
        DriverManager.getConnection(jdbcUrl).also { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA busy_timeout = 5000")
            }
        }

    internal fun <T> transaction(block: (Connection) -> T): T {
        openConnection().use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                return result
            } catch (error: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackError: SQLException) {
                    error.addSuppressed(rollbackError)
                }
                throw error
            }
        }
    }
}
