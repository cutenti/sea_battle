package battleship.cli

import battleship.core.BoardFactory
import battleship.core.EloRatingCalculator
import battleship.core.GameEngine
import battleship.core.GameRulesValidatorImpl
import battleship.core.GameService
import battleship.core.InMemoryGameRepository
import battleship.core.InMemoryPlayerRepository
import battleship.core.InvalidGameSetupException
import battleship.core.Position
import java.util.Scanner

fun main() {
    val validator = GameRulesValidatorImpl()
    val service = GameService(
        players = InMemoryPlayerRepository(),
        games = InMemoryGameRepository(),
        engine = GameEngine(validator, EloRatingCalculator()),
        validator = validator,
    )

    println(Messages.WELCOME)
    println(Messages.COMMANDS_LIST)

    Scanner(System.`in`).use { scanner ->
        runLoop(service, validator, scanner)
    }
}

private fun runLoop(service: GameService, validator: GameRulesValidatorImpl, scanner: Scanner) {
    while (true) {
        print(Messages.PROMPT)
        if (!scanner.hasNextLine()) break
        when (val parsed = InputParser.parse(scanner.nextLine())) {
            is ParsedCommand.AddPlayer -> {
                val player = service.addPlayer(parsed.name)
                println(Messages.PLAYER_ADDED.format(player.name, player.id))
            }
            ParsedCommand.ListPlayers -> println(SessionPrinter.formatPlayers(service.listPlayers()))
            is ParsedCommand.StartGame -> startGame(service, validator, parsed)
            is ParsedCommand.Show -> showGame(service, parsed.gameId)
            is ParsedCommand.Move -> move(service, parsed)
            is ParsedCommand.Error -> println(parsed.message)
            ParsedCommand.Exit -> return
        }
    }
}

private fun startGame(
    service: GameService,
    validator: GameRulesValidatorImpl,
    parsed: ParsedCommand.StartGame,
) {
    val first = service.findPlayer(parsed.player1Id)
    val second = service.findPlayer(parsed.player2Id)
    if (first == null || second == null) {
        println(Messages.PLAYERS_NOT_FOUND)
        return
    }
    try {
        val session = service.startGame(
            player1 = first,
            player2 = second,
            board1 = BoardFactory.createRandomBoard(validator),
            board2 = BoardFactory.createRandomBoard(validator),
        )
        println(Messages.GAME_STARTED.format(session.id, session.currentTurnPlayerId))
        println(SessionPrinter.format(session))
    } catch (error: InvalidGameSetupException) {
        println(error.message)
    }
}

private fun showGame(service: GameService, gameId: String) {
    val session = service.findGame(gameId)
    if (session == null) {
        println(Messages.GAME_NOT_FOUND)
        return
    }
    println(SessionPrinter.format(session))
}

private fun move(service: GameService, parsed: ParsedCommand.Move) {
    val session = service.findGame(parsed.gameId)
    if (session == null) {
        println(Messages.GAME_NOT_FOUND)
        return
    }
    val result = service.recordMove(session, parsed.playerId, Position(parsed.x, parsed.y))
    println(Messages.MOVE_RESULT.format(result.name, session.currentTurnPlayerId))
    SessionPrinter.finishedLine(session)?.let(::println)
}
