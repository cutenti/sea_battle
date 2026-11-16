package battleship.core

import kotlin.random.Random

object BoardFactory {
    private val shipSizes = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

    fun createValidClassicalBoard(): Board {
        return Board(
            listOf(
                Ship(listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(0, 3))),
                Ship(listOf(Position(2, 0), Position(2, 1), Position(2, 2))),
                Ship(listOf(Position(4, 0), Position(4, 1), Position(4, 2))),
                Ship(listOf(Position(6, 0), Position(6, 1))),
                Ship(listOf(Position(8, 0), Position(8, 1))),
                Ship(listOf(Position(0, 6), Position(1, 6))),
                Ship(listOf(Position(3, 6))),
                Ship(listOf(Position(5, 6))),
                Ship(listOf(Position(7, 6))),
                Ship(listOf(Position(9, 6))),
            ),
        )
    }

    /**
     * Корабли ставятся по одному. Каждый кандидат проверяется через [IGameRulesValidator.canPlaceShip]:
     * форма, границы и касания. Полный комплект в этот момент ещё не собран, поэтому
     * [IGameRulesValidator.validateBoard] здесь не вызывается — из-за этого генератор раньше зацикливался.
     * Готовая доска проверяется целиком. Число попыток ограничено.
     */
    fun createRandomBoard(
        validator: IGameRulesValidator = GameRulesValidatorImpl(),
        random: Random = Random,
        maxAttempts: Int = 300,
    ): Board {
        repeat(maxAttempts) {
            val ships = mutableListOf<Ship>()
            var failed = false

            for (size in shipSizes) {
                var placed = false
                var tries = 0
                while (!placed && tries < 100) {
                    tries++
                    val horizontal = random.nextBoolean()
                    val startX = if (horizontal) random.nextInt(10 - size + 1) else random.nextInt(10)
                    val startY = if (horizontal) random.nextInt(10) else random.nextInt(10 - size + 1)
                    val positions = (0 until size).map { offset ->
                        if (horizontal) Position(startX + offset, startY) else Position(startX, startY + offset)
                    }
                    val candidate = Ship(positions)
                    if (validator.canPlaceShip(ships, candidate)) {
                        ships.add(candidate)
                        placed = true
                    }
                }
                if (!placed) {
                    failed = true
                    break
                }
            }

            if (!failed && ships.size == shipSizes.size) {
                val board = Board(ships.toList())
                if (validator.validateBoard(board)) return board
            }
        }
        throw IllegalStateException("Не удалось собрать поле за $maxAttempts попыток")
    }
}
