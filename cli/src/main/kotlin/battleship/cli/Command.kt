package battleship.cli

enum class Command(val rawString: String) {
    ADD_PLAYER("add_player"),
    LIST_PLAYERS("list_players"),
    START_GAME("start_game"),
    SHOW("show"),
    MOVE("move"),
    EXIT("exit");

    companion object {
        fun fromString(str: String): Command? = entries.find { it.rawString == str }
    }
}
