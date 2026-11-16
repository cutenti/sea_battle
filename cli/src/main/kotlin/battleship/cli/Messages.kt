package battleship.cli

object Messages {
    const val WELCOME = "Система администрирования «Морской бой» запущена."
    const val COMMANDS_LIST =
        "Команды: add_player <name>, list_players, start_game <id1> <id2>, show <game_id>, move <game_id> <player_id> <x> <y>, exit"
    const val PROMPT = "> "
    const val PLAYER_ADDED = "Игрок %s добавлен. ID: %s"
    const val NO_PLAYERS = "Игроков пока нет."
    const val PLAYERS_NOT_FOUND = "Игроки не найдены."
    const val GAME_STARTED = "Игра %s началась. Ходит: %s"
    const val GAME_NOT_FOUND = "Игра не найдена."
    const val MOVE_RESULT = "Результат: %s. Очередь хода: %s"
    const val GAME_FINISHED = "Партия завершена. Победитель: %s. Рейтинг: %s %d, %s %d"
    const val UNKNOWN_COMMAND = "Неизвестная команда"
    const val INVALID_ARGUMENTS = "Неверное количество аргументов"
    const val INVALID_COORDINATES = "Координаты должны быть целыми числами"
    const val BOARD_LEGEND = "S корабль, X попадание, # потоплен, o промах, . пусто"
    const val NO_MOVES = "Ходов пока нет."
}
