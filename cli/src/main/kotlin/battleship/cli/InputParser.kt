package battleship.cli

sealed interface ParsedCommand {
    data class AddPlayer(val name: String) : ParsedCommand
    data object ListPlayers : ParsedCommand
    data class StartGame(val player1Id: String, val player2Id: String) : ParsedCommand
    data class Show(val gameId: String) : ParsedCommand
    data class Move(val gameId: String, val playerId: String, val x: Int, val y: Int) : ParsedCommand
    data object Exit : ParsedCommand
    data class Error(val message: String) : ParsedCommand
}

object InputParser {
    fun parse(input: String): ParsedCommand {
        val parts = input.trim().split("\\s+".toRegex())
        if (parts.isEmpty() || parts[0].isEmpty()) {
            return ParsedCommand.Error(Messages.UNKNOWN_COMMAND)
        }

        val cmd = Command.fromString(parts[0])
            ?: return ParsedCommand.Error(Messages.UNKNOWN_COMMAND)

        return when (cmd) {
            Command.ADD_PLAYER -> {
                if (parts.size < 2) ParsedCommand.Error(Messages.INVALID_ARGUMENTS)
                else ParsedCommand.AddPlayer(parts[1])
            }
            Command.LIST_PLAYERS -> ParsedCommand.ListPlayers
            Command.START_GAME -> {
                if (parts.size < 3) ParsedCommand.Error(Messages.INVALID_ARGUMENTS)
                else ParsedCommand.StartGame(parts[1], parts[2])
            }
            Command.SHOW -> {
                if (parts.size < 2) ParsedCommand.Error(Messages.INVALID_ARGUMENTS)
                else ParsedCommand.Show(parts[1])
            }
            Command.MOVE -> {
                if (parts.size < 5) return ParsedCommand.Error(Messages.INVALID_ARGUMENTS)
                val x = parts[3].toIntOrNull()
                val y = parts[4].toIntOrNull()
                if (x == null || y == null) {
                    ParsedCommand.Error(Messages.INVALID_COORDINATES)
                } else {
                    ParsedCommand.Move(parts[1], parts[2], x, y)
                }
            }
            Command.EXIT -> ParsedCommand.Exit
        }
    }
}
