package battleship.cli

import battleship.core.*
import java.util.Scanner
import java.util.UUID

fun main() {
    val playerRepo = InMemoryPlayerRepository()
    val gameRepo = InMemoryGameRepository()
    val validator = GameRulesValidatorImpl()
    val engine = GameEngine(validator)

    println(Messages.WELCOME)
    println(Messages.COMMANDS_LIST)

    Scanner(System.`in`).use { scanner ->
        while (scanner.hasNextLine()) {
            print(Messages.PROMPT)
            val line = scanner.nextLine()

            when (val parsed = InputParser.parse(line)) {
                is ParsedCommand.AddPlayer -> {
                    val id = UUID.randomUUID().toString().take(4)
                    playerRepo.save(Player(id, parsed.name))
                    println(Messages.PLAYER_ADDED.format(parsed.name, id))
                }
                is ParsedCommand.StartGame -> {
                    val p1 = playerRepo.findById(parsed.player1Id)
                    val p2 = playerRepo.findById(parsed.player2Id)
                    if (p1 != null && p2 != null) {
                        val gameId = UUID.randomUUID().toString().take(4)
                        val b1 = BoardFactory.createValidClassicalBoard()
                        val b2 = BoardFactory.createValidClassicalBoard()
                        val session = GameSession(gameId, p1, p2, b1, b2)
                        gameRepo.save(session)
                        println(Messages.GAME_STARTED.format(gameId, session.currentTurnPlayerId))
                    } else {
                        println(Messages.PLAYERS_NOT_FOUND)
                    }
                }
                is ParsedCommand.Move -> {
                    val session = gameRepo.findById(parsed.gameId)
                    if (session != null) {
                        val result = engine.processMove(session, parsed.playerId, Position(parsed.x, parsed.y))
                        println(Messages.MOVE_RESULT.format(result.name, session.currentTurnPlayerId))
                    } else {
                        println(Messages.GAME_NOT_FOUND)
                    }
                }
                is ParsedCommand.Error -> println(parsed.message)
                is ParsedCommand.Exit -> return
            }
        }
    }
}