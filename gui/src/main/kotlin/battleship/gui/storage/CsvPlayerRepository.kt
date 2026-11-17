package battleship.gui.storage

import battleship.core.IPlayerRepository
import battleship.core.Player
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class CsvPlayerRepository(private val file: Path) : IPlayerRepository {
    private val players = linkedMapOf<String, Player>()

    init {
        if (Files.exists(file)) {
            readPlayers()
        }
    }

    override fun save(player: Player) {
        require('\n' !in player.name && '\r' !in player.name) {
            "Имя игрока не может содержать перевод строки"
        }

        val updated = LinkedHashMap(players)
        updated[player.id] = player
        writePlayers(updated.values)
        players.clear()
        players.putAll(updated)
    }

    override fun findById(id: String): Player? = players[id]

    override fun findAll(): List<Player> = players.values.toList()

    private fun readPlayers() {
        val text = Files.readString(file, StandardCharsets.UTF_8)
        if (text.isBlank()) return

        val records = parseCsv(text)
        if (records.firstOrNull() != listOf("id", "name", "rating")) {
            throw IOException("Некорректный заголовок CSV-файла игроков: $file")
        }

        records.drop(1).forEachIndexed { index, record ->
            if (record.size != 3 || record[0].isBlank()) {
                throw IOException("Некорректная запись игрока в строке ${index + 2}: $file")
            }
            val rating = record[2].toIntOrNull()
                ?: throw IOException("Некорректный рейтинг в строке ${index + 2}: $file")
            if (rating < 0 || players.containsKey(record[0])) {
                throw IOException("Некорректная или повторяющаяся запись в строке ${index + 2}: $file")
            }
            players[record[0]] = Player(record[0], record[1], rating)
        }
    }

    private fun writePlayers(entries: Collection<Player>) {
        val parent = file.toAbsolutePath().parent
        Files.createDirectories(parent)
        val contents = buildString {
            appendLine("id,name,rating")
            entries.forEach { player ->
                append(csvField(player.id))
                append(',')
                append(csvField(player.name))
                append(',')
                append(csvField(player.rating.toString()))
                appendLine()
            }
        }
        val temporary = Files.createTempFile(parent, file.fileName.toString(), ".tmp")
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8)
            try {
                Files.move(
                    temporary,
                    file.toAbsolutePath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun csvField(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var afterQuote = false
        var index = 0

        fun finishField() {
            row += field.toString()
            field.setLength(0)
            afterQuote = false
        }

        fun finishRow() {
            finishField()
            if (row.any(String::isNotEmpty)) rows += row.toList()
            row.clear()
        }

        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes && char == '"' && text.getOrNull(index + 1) == '"' -> {
                    field.append('"')
                    index++
                }
                inQuotes && char == '"' -> {
                    inQuotes = false
                    afterQuote = true
                }
                inQuotes -> field.append(char)
                afterQuote && char != ',' && char != '\n' && char != '\r' ->
                    throw IOException("Некорректный CSV: символ после закрывающей кавычки")
                char == '"' && field.isEmpty() -> inQuotes = true
                char == ',' -> finishField()
                char == '\n' || char == '\r' -> {
                    finishRow()
                    if (char == '\r' && text.getOrNull(index + 1) == '\n') index++
                }
                char == '"' -> throw IOException("Некорректный CSV: кавычка внутри поля")
                else -> field.append(char)
            }
            index++
        }
        if (inQuotes) throw IOException("Некорректный CSV: незакрытая кавычка")
        if (field.isNotEmpty() || row.isNotEmpty() || afterQuote) finishRow()
        return rows
    }
}
