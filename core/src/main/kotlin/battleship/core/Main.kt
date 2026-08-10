import java.util.Scanner
import java.util.UUID

fun main() {
    val scanner = Scanner(System.`in`)
    val playerRepo = InMemoryPlayerRepository()
    val gameRepo = InMemoryGameRepository()
    val validator = GameRulesValidatorImpl()
    val engine = GameEngine(validator)

    println("Система администрирования 'Морской бой' запущена.")
    println("Команды: add_player <name>, start_game <id1> <id2>, move <game_id> <player_id> <x> <y>, exit")

    while (true) {
        print("> ")
        val input = scanner.nextLine().trim().split(" ")
        when (input[0]) {
            "add_player" -> {
                val id = UUID.randomUUID().toString().take(4)
                playerRepo.save(Player(id, input[1]))
                println("Игрок ${input[1]} добавлен. ID: $id")
            }
            "start_game" -> {
                val p1 = playerRepo.findById(input[1])
                val p2 = playerRepo.findById(input[2])
                if (p1 != null && p2 != null) {
                    val gameId = UUID.randomUUID().toString().take(4)
                    // Для примера создаем доски с одним кораблем (1x1) для каждого
                    val b1 = Board(listOf(Ship(listOf(Position(0, 0)))))
                    val b2 = Board(listOf(Ship(listOf(Position(1, 1)))))
                    val session = GameSession(gameId, p1, p2, b1, b2)
                    gameRepo.save(session)
                    println("Игра $gameId началась! Ходит: ${session.currentTurnPlayerId}")
                } else {
                    println("Игроки не найдены.")
                }
            }
            "move" -> {
                val session = gameRepo.findById(input[1])
                if (session != null) {
                    val x = input[3].toInt()
                    val y = input[4].toInt()
                    val result = engine.processMove(session, input[2], Position(x, y))
                    println("Результат: $result. Очередь хода: ${session.currentTurnPlayerId}")
                } else {
                    println("Игра не найдена.")
                }
            }
            "exit" -> return
            else -> println("Неизвестная команда")
        }
    }
}