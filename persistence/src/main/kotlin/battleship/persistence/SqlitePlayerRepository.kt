package battleship.persistence

import battleship.core.IPlayerRepository
import battleship.core.Player
import java.sql.Connection

class SqlitePlayerRepository(private val database: SqliteDatabase) : IPlayerRepository {
    override fun save(player: Player) {
        database.transaction { connection -> upsertPlayer(connection, player) }
    }

    fun saveAll(players: Iterable<Player>) {
        database.transaction { connection -> players.forEach { upsertPlayer(connection, it) } }
    }

    override fun findById(id: String): Player? = database.openConnection().use { connection ->
        connection.prepareStatement("SELECT id, name, rating FROM players WHERE id = ?").use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { result ->
                if (result.next()) Player(result.getString("id"), result.getString("name"), result.getInt("rating"))
                else null
            }
        }
    }

    override fun findAll(): List<Player> = database.openConnection().use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT id, name, rating FROM players ORDER BY rowid").use { result ->
                buildList {
                    while (result.next()) {
                        add(Player(result.getString("id"), result.getString("name"), result.getInt("rating")))
                    }
                }
            }
        }
    }
}

internal fun upsertPlayer(connection: Connection, player: Player) {
    connection.prepareStatement(
        """
        INSERT INTO players(id, name, rating) VALUES (?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET name = excluded.name, rating = excluded.rating
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, player.id)
        statement.setString(2, player.name)
        statement.setInt(3, player.rating)
        statement.executeUpdate()
    }
    connection.prepareStatement(
        "INSERT INTO player_statistics(player_id) VALUES (?) ON CONFLICT(player_id) DO NOTHING",
    ).use { statement ->
        statement.setString(1, player.id)
        statement.executeUpdate()
    }
}
