package battleship.core

import kotlin.random.Random

object BoardFactory {
    private val SHIP_SIZES = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

    fun createValidClassicalBoard(): Board {
        return Board(listOf(
            Ship(listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(0, 3))),
            Ship(listOf(Position(2, 0), Position(2, 1), Position(2, 2))),
            Ship(listOf(Position(4, 0), Position(4, 1), Position(4, 2))),
            Ship(listOf(Position(6, 0), Position(6, 1))),
            Ship(listOf(Position(8, 0), Position(8, 1))),
            Ship(listOf(Position(0, 6), Position(1, 6))),
            Ship(listOf(Position(3, 6))),
            Ship(listOf(Position(5, 6))),
            Ship(listOf(Position(7, 6))),
            Ship(listOf(Position(9, 6)))
        ))
    }

    fun createRandomBoard(): Board {
        val validator = GameRulesValidatorImpl()
        while (true) {
            val ships = mutableListOf<Ship>()
            var failed = false

            for (size in SHIP_SIZES) {
                var placed = false
                var attempts = 0
                
                while (!placed && attempts < 100) {
                    attempts++
                    val horizontal = Random.nextBoolean()
                    val startX = if (horizontal) Random.nextInt(10 - size + 1) else Random.nextInt(10)
                    val startY = if (horizontal) Random.nextInt(10) else Random.nextInt(10 - size + 1)

                    val positions = (0 until size).map { i ->
                        if (horizontal) Position(startX + i, startY) else Position(startX, startY + i)
                    }

                    val candidateShip = Ship(positions)
                    val tempBoard = Board(ships + candidateShip)

                    if (validator.validateBoard(tempBoard)) {
                        ships.add(candidateShip)
                        placed = true
                    }
                }

                if (!placed) {
                    failed = true
                    break
                }
            }

            if (!failed && ships.size == 10) {
                return Board(ships)
            }
        }
    }
}