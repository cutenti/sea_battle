package battleship.cli

object Messages {
    const val WELCOME = "Система администрирования 'Морской бой' запущена."
    const val COMMANDS_LIST = "Команды: add_player <name>, start_game <id1> <id2>, move <game_id> <player_id> <x> <y>, exit"
    const val PROMPT = "> "
    const val PLAYER_ADDED = "Игрок %s добавлен. ID: %s"
    const val PLAYERS_NOT_FOUND = "Игроки не найдены."
    const val INVALID_BOARD = "Ошибка: сгенерировано невалидное поле для игры."
    const val GAME_STARTED = "Игра %s началась! Ходит: %s"
    const val GAME_NOT_FOUND = "Игра не найдена."
    const val MOVE_RESULT = "Результат: %s. Очередь хода: %s"
    const val UNKNOWN_COMMAND = "Неизвестная команда"
    const val INVALID_ARGUMENTS = "Неверное количество аргументов"
    const val INVALID_COORDINATES = "Координаты должны быть целыми числами"
    const val ERROR_PREFIX = "Системная ошибка: "
}