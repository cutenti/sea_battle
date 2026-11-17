package battleship.cli

enum class Command(val rawString: String) {
    ADD_PLAYER("add_player"),
    START_GAME("start_game"),
    MOVE("move"),
    EXIT("exit");

    companion object {
        fun fromString(str: String): Command? = entries.find { it.rawString == str }
    }
}