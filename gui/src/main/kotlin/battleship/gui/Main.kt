package battleship.gui

import battleship.core.EloRatingCalculator
import battleship.core.GameEngine
import battleship.core.GameRulesValidatorImpl
import battleship.core.GameService
import battleship.persistence.SqliteDatabase
import battleship.persistence.SqliteGameRepository
import battleship.persistence.SqlitePlayerRepository
import battleship.gui.storage.CsvPlayerRepository
import java.awt.EventQueue
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.sql.SQLException
import javax.swing.JOptionPane
import javax.swing.UIManager

fun main() {
    EventQueue.invokeLater {
        val validator = GameRulesValidatorImpl()
        val service = try {
            val dataDirectory = Path.of(System.getProperty("user.home"), ".sea-battle")
            val database = SqliteDatabase(dataDirectory.resolve("battleship.db"))
            val playerRepository = SqlitePlayerRepository(database)
            val legacyPlayerFile = dataDirectory.resolve("players.csv")
            if (Files.exists(legacyPlayerFile)) {
                val existingIds = playerRepository.findAll().map { it.id }.toSet()
                val missingPlayers = CsvPlayerRepository(legacyPlayerFile).findAll()
                    .filterNot { it.id in existingIds }
                playerRepository.saveAll(missingPlayers)
            }
            GameService(
                players = playerRepository,
                games = SqliteGameRepository(database),
                engine = GameEngine(validator, EloRatingCalculator()),
                validator = validator,
            )
        } catch (error: IOException) {
            showDatabaseError(error.message)
            return@invokeLater
        } catch (error: SQLException) {
            showDatabaseError(error.message)
            return@invokeLater
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: ReflectiveOperationException) {
            // Swing's cross-platform look and feel remains available.
        } catch (_: javax.swing.UnsupportedLookAndFeelException) {
            // Swing's cross-platform look and feel remains available.
        }
        BattleshipFrame(GameController(service, validator)).isVisible = true
    }
}

private fun showDatabaseError(message: String?) {
    JOptionPane.showMessageDialog(
        null,
        message ?: "Не удалось открыть базу данных",
        "Ошибка базы данных",
        JOptionPane.ERROR_MESSAGE,
    )
}
