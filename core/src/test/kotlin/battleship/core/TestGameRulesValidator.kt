package battleship.core

internal class PermissiveBoardRulesValidator : IGameRulesValidator by GameRulesValidatorImpl() {
    override fun validateBoard(board: Board): Boolean = true
}
