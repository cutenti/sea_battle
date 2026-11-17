package battleship.gui

import battleship.core.Board
import battleship.core.GameSession
import battleship.core.GameState
import battleship.core.Player
import battleship.core.Position
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.io.IOException
import java.sql.SQLException
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.Timer
import javax.swing.table.AbstractTableModel

internal class BattleshipFrame(private val controller: GameController) : JFrame("Морской бой") {
    private val nameField = JTextField(12)
    private val firstPlayer = JComboBox<Player>()
    private val secondPlayer = JComboBox<Player>()
    private val startButton = JButton("Начать партию")
    private val togglePlayersButton = JButton("Скрыть игроков")
    private val ownBoard = BoardGrid()
    private val targetBoard = BoardGrid(::shoot)
    private val ownTitle = JLabel("Поле игрока")
    private val targetTitle = JLabel("Поле цели")
    private val turnLabel = JLabel("Добавьте игроков и начните партию")
    private val scoreLabel = JLabel("Счёт: —")
    private val timerLabel = JLabel("Время: 00:00")
    private val history = JTextArea(6, 40).apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    private val statisticsModel = StatisticsTableModel()
    private val playersTable = JTable(statisticsModel)
    private val gamesModel = GameHistoryTableModel()
    private val gamesTable = JTable(gamesModel)
    private val playersPane = JTabbedPane().apply {
        addTab("Игроки", JScrollPane(playersTable))
        addTab("История партий", JScrollPane(gamesTable))
    }
    private val clock = Timer(1000) { updateTimer() }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(900, 650)
        setSize(1100, 760)
        setLocationRelativeTo(null)
        layout = BorderLayout(8, 8)
        add(toolbar(), BorderLayout.NORTH)
        add(centerPanel(), BorderLayout.CENTER)
        add(footer(), BorderLayout.SOUTH)
        configureSelectors()
        refreshPlayers()
        refreshGame()
        refreshStatistics()
        clock.start()
    }

    private fun toolbar() = JPanel().apply {
        border = BorderFactory.createEmptyBorder(8, 8, 0, 8)
        add(JLabel("Имя:"))
        add(nameField)
        add(JButton("Добавить").apply {
            addActionListener { addPlayer() }
        })
        nameField.addActionListener { addPlayer() }
        add(JLabel("Партия:"))
        add(firstPlayer)
        add(JLabel("против"))
        add(secondPlayer)
        add(startButton)
        add(togglePlayersButton)
        startButton.addActionListener { startGame() }
        togglePlayersButton.addActionListener {
            playersPane.isVisible = !playersPane.isVisible
            togglePlayersButton.text = if (playersPane.isVisible) "Скрыть игроков" else "Показать игроков"
            revalidate()
        }
    }

    private fun centerPanel(): JPanel {
        val boards = JPanel(GridLayout(1, 2, 12, 0)).apply {
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
            add(boardSection(ownTitle, ownBoard))
            add(boardSection(targetTitle, targetBoard))
        }
        return JPanel(BorderLayout()).apply {
            add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boards, playersPane).apply {
                resizeWeight = 0.8
                dividerLocation = 820
            }, BorderLayout.CENTER)
        }
    }

    private fun boardSection(title: JLabel, board: BoardGrid) = JPanel(BorderLayout(0, 8)).apply {
        title.horizontalAlignment = SwingConstants.CENTER
        title.font = title.font.deriveFont(Font.BOLD, 16f)
        add(title, BorderLayout.NORTH)
        add(board, BorderLayout.CENTER)
    }

    private fun footer() = JPanel(BorderLayout(8, 8)).apply {
        border = BorderFactory.createEmptyBorder(0, 12, 12, 12)
        add(JPanel().apply {
            add(turnLabel)
            add(scoreLabel)
            add(timerLabel)
        }, BorderLayout.NORTH)
        add(JScrollPane(history), BorderLayout.CENTER)
    }

    private fun configureSelectors() {
        val renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): java.awt.Component = super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus,
            ).also { label ->
                if (label is JLabel) {
                    label.text = (value as? Player)?.let { "${it.name} (${it.id})" }.orEmpty()
                }
            }
        }
        firstPlayer.renderer = renderer
        secondPlayer.renderer = renderer
        playersTable.fillsViewportHeight = true
        playersTable.preferredScrollableViewportSize = Dimension(280, 400)
        gamesTable.fillsViewportHeight = true
        gamesTable.preferredScrollableViewportSize = Dimension(280, 400)
    }

    private fun addPlayer() {
        val name = nameField.text.trim()
        if (name.isEmpty()) return showError("Введите имя игрока")
        try {
            controller.addPlayer(name)
            nameField.text = ""
            refreshPlayers()
            refreshStatistics()
        } catch (error: IOException) {
            showError(error.message ?: "Не удалось сохранить реестр игроков")
        } catch (error: SQLException) {
            showError(error.message ?: "Не удалось сохранить игрока в базе данных")
        } catch (error: IllegalArgumentException) {
            showError(error.message ?: "Некорректное имя игрока")
        }
    }

    private fun startGame() {
        val first = firstPlayer.selectedItem as? Player
        val second = secondPlayer.selectedItem as? Player
        if (first == null || second == null) return showError("Добавьте двух игроков")
        if (first.id == second.id) return showError("Выберите двух разных игроков")
        try {
            controller.startGame(first.id, second.id)
            refreshGame()
            refreshStatistics()
        } catch (error: IllegalArgumentException) {
            showError(error.message ?: "Не удалось начать партию")
        } catch (error: IllegalStateException) {
            showError(error.message ?: "Не удалось создать игровое поле")
        } catch (error: SQLException) {
            showError(error.message ?: "Не удалось сохранить партию в базе данных")
        }
    }

    private fun shoot(position: Position) {
        try {
            controller.shoot(position)
            refreshGame()
            refreshStatistics()
        } catch (error: IOException) {
            refreshGame()
            refreshStatistics()
            showError(error.message ?: "Ход выполнен, но не удалось сохранить рейтинг")
        } catch (error: SQLException) {
            refreshGame()
            refreshStatistics()
            showError(error.message ?: "Ход выполнен, но не удалось сохранить партию")
        } catch (error: IllegalStateException) {
            showError(error.message ?: "Нельзя выполнить ход")
        }
    }

    private fun refreshPlayers() {
        val players = controller.players().toTypedArray()
        firstPlayer.model = DefaultComboBoxModel(players)
        secondPlayer.model = DefaultComboBoxModel(players)
        if (players.size > 1) secondPlayer.selectedIndex = 1
        startButton.isEnabled = players.size > 1
    }

    private fun refreshGame() {
        val session = controller.currentSession
        if (session == null) {
            ownTitle.text = "Поле игрока"
            targetTitle.text = "Поле цели"
            ownBoard.show(null, revealShips = true, canShoot = false)
            targetBoard.show(null, revealShips = false, canShoot = false)
            turnLabel.text = "Добавьте игроков и начните партию"
            scoreLabel.text = "Счёт: —"
            history.text = ""
            return
        }

        val shooter = currentPlayer(session)
        val target = if (shooter.id == session.player1.id) session.player2 else session.player1
        val own = if (shooter.id == session.player1.id) session.board1 else session.board2
        val targetState = if (target.id == session.player1.id) session.board1 else session.board2
        ownTitle.text = "Поле ${shooter.name}"
        targetTitle.text = "Цель: ${target.name}"
        ownBoard.show(own, revealShips = true, canShoot = false)
        targetBoard.show(targetState, revealShips = false, canShoot = session.state == GameState.IN_PROGRESS)
        turnLabel.text = if (session.state == GameState.FINISHED) {
            "Партия завершена. Победитель: ${playerName(session, session.winnerId)}"
        } else {
            "Ход: ${shooter.name}"
        }
        val firstHits = session.board2.ships.sumOf { it.hits.size }
        val secondHits = session.board1.ships.sumOf { it.hits.size }
        scoreLabel.text = "Счёт: ${session.player1.name} $firstHits — ${session.player2.name} $secondHits" +
            "  |  Рейтинг: ${session.player1.rating} / ${session.player2.rating}"
        history.text = session.moves.mapIndexed { index, move ->
            "${index + 1}. ${playerName(session, move.playerId)} " +
                "(${move.position.x}, ${move.position.y}) — ${move.result}"
        }.joinToString("\n").ifEmpty { "Журнал ходов пока пуст." }
        history.caretPosition = history.document.length
        updateTimer()
    }

    private fun refreshStatistics() {
        statisticsModel.update(controller.statistics())
        gamesModel.update(controller.games())
    }

    private fun updateTimer() {
        val seconds = controller.elapsedSeconds()
        timerLabel.text = "Время: %02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun currentPlayer(session: GameSession): Player =
        if (session.currentTurnPlayerId == session.player1.id) session.player1 else session.player2

    private fun playerName(session: GameSession, id: String?): String = when (id) {
        session.player1.id -> session.player1.name
        session.player2.id -> session.player2.name
        else -> id ?: "неизвестен"
    }

    private fun showError(message: String) {
        JOptionPane.showMessageDialog(this, message, "Морской бой", JOptionPane.WARNING_MESSAGE)
    }
}

private class BoardGrid(onShoot: ((Position) -> Unit)? = null) : JPanel(GridLayout(11, 11)) {
    private val buttons = Array(10) { y ->
        Array(10) { x ->
            JButton().apply {
                font = font.deriveFont(Font.BOLD, 13f)
                margin = java.awt.Insets(0, 0, 0, 0)
                addActionListener { onShoot?.invoke(Position(x, y)) }
            }
        }
    }

    init {
        border = BorderFactory.createLineBorder(Color.GRAY)
        add(JLabel(""))
        for (x in 0..9) add(axisLabel(x))
        for (y in 0..9) {
            add(axisLabel(y))
            for (x in 0..9) add(buttons[y][x])
        }
    }

    fun show(board: Board?, revealShips: Boolean, canShoot: Boolean) {
        for (y in 0..9) {
            for (x in 0..9) {
                val position = Position(x, y)
                val cell = buttons[y][x]
                val ship = board?.ships?.find { position in it.positions }
                val shot = position in board?.shots.orEmpty()
                val mark = when {
                    ship != null && shot && ship.isSunk() -> "#"
                    ship != null && shot -> "X"
                    shot -> "o"
                    ship != null && revealShips -> "S"
                    else -> ""
                }
                cell.text = mark
                cell.isEnabled = canShoot && board != null && !shot
                cell.background = when (mark) {
                    "#", "X" -> Color(0xE5, 0x7C, 0x73)
                    "o" -> Color(0x9E, 0xD6, 0xF5)
                    "S" -> Color(0xB9, 0xC9, 0xD8)
                    else -> Color.WHITE
                }
                cell.toolTipText = "(${position.x}, ${position.y})"
            }
        }
    }

    private fun axisLabel(value: Int) = JLabel(value.toString(), SwingConstants.CENTER)
}

private class StatisticsTableModel : AbstractTableModel() {
    private val columns = listOf("Игрок", "Рейтинг", "Игр", "Побед", "Поражений")
    private var rows = emptyList<PlayerStatistics>()

    fun update(values: List<PlayerStatistics>) {
        rows = values
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val row = rows[rowIndex]
        return when (columnIndex) {
            0 -> row.player.name
            1 -> row.player.rating
            2 -> row.gamesPlayed
            3 -> row.wins
            4 -> row.losses
            else -> error("Unknown column $columnIndex")
        }
    }
}

private class GameHistoryTableModel : AbstractTableModel() {
    private val columns = listOf("Партия", "Игроки", "Состояние", "Победитель", "Ходов")
    private var rows = emptyList<GameSession>()

    fun update(values: List<GameSession>) {
        rows = values.asReversed()
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val game = rows[rowIndex]
        return when (columnIndex) {
            0 -> game.id
            1 -> "${game.player1.name} — ${game.player2.name}"
            2 -> game.state.name
            3 -> when (game.winnerId) {
                game.player1.id -> game.player1.name
                game.player2.id -> game.player2.name
                else -> "—"
            }
            4 -> game.moves.size
            else -> error("Unknown column $columnIndex")
        }
    }
}
