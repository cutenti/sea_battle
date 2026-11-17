package battleship.cli

import battleship.core.*
import java.util.Scanner
import java.util.UUID

fun main() {
    val playerRepo: IPlayerRepository = InMemoryPlayerRepository()
    val gameRepo: IGameRepository = InMemoryGameRepository()
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
                        val b1 = BoardFactory.createRandomBoard()
                        val b2 = BoardFactory.createRandomBoard()
                        val session = GameSession(gameId, p1, p2, b1, b2)
                        if (engine.startGame(session)) {
                            gameRepo.save(session)
                            println(Messages.GAME_STARTED.format(gameId, session.currentTurnPlayerId))
                            print(BoardRenderer.renderForPlayer(session, session.currentTurnPlayerId))
                            println(Messages.MOVE_HISTORY)
                            println(Messages.NO_MOVES)
                        } else {
                            println(Messages.INVALID_BOARD)
                        }
                    } else {
                        println(Messages.PLAYERS_NOT_FOUND)
                    }
                }
                is ParsedCommand.Move -> {
                    val session = gameRepo.findById(parsed.gameId)
                    if (session != null) {
                        val result = engine.processMove(session, parsed.playerId, Position(parsed.x, parsed.y))
                        println(Messages.MOVE_RESULT.format(result.name))
                        if (session.state == GameState.FINISHED) {
                            println(Messages.GAME_FINISHED.format(session.winnerId))
                        } else {
                            println(Messages.CURRENT_TURN.format(session.currentTurnPlayerId))
                        }
                        gameRepo.save(session)
                        val viewerId = when (parsed.playerId) {
                            session.player1.id, session.player2.id -> parsed.playerId
                            else -> session.winnerId ?: session.currentTurnPlayerId
                        }
                        print(BoardRenderer.renderForPlayer(session, viewerId))
                        println(Messages.MOVE_HISTORY)
                        if (session.moves.isEmpty()) {
                            println(Messages.NO_MOVES)
                        } else {
                            session.moves.forEachIndexed { index, move ->
                                val playerName = when (move.playerId) {
                                    session.player1.id -> session.player1.name
                                    session.player2.id -> session.player2.name
                                    else -> move.playerId
                                }
                                println(Messages.MOVE_ENTRY.format(
                                    index + 1,
                                    playerName,
                                    move.position.x,
                                    move.position.y,
                                    move.result.name
                                ))
                            }
                        }
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