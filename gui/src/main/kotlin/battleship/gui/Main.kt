package battleship.gui

import battleship.core.EloRatingCalculator
import battleship.core.GameEngine
import battleship.core.GameRulesValidatorImpl
import battleship.core.GameService
import battleship.core.InMemoryGameRepository
import battleship.gui.storage.CsvPlayerRepository
import java.awt.EventQueue
import java.io.IOException
import java.nio.file.Path
import javax.swing.JOptionPane
import javax.swing.UIManager

fun main() {
    EventQueue.invokeLater {
        val validator = GameRulesValidatorImpl()
        val registryPath = Path.of(System.getProperty("user.home"), ".sea-battle", "players.csv")
        val playerRepository = try {
            CsvPlayerRepository(registryPath)
        } catch (error: IOException) {
            JOptionPane.showMessageDialog(
                null,
                error.message ?: "Не удалось загрузить реестр игроков",
                "Ошибка реестра игроков",
                JOptionPane.ERROR_MESSAGE,
            )
            return@invokeLater
        }
        val service = GameService(
            players = playerRepository,
            games = InMemoryGameRepository(),
            engine = GameEngine(validator, EloRatingCalculator()),
            validator = validator,
        )
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
