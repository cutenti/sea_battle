package battleship.cli

import battleship.core.Board
import battleship.core.GameSession
import battleship.core.Player
import battleship.core.Position
import battleship.core.Ship
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardRendererTest {
    @Test
    fun `renderForPlayer shows own ships and hides opponent ships`() {
        val ownBoard = Board(listOf(Ship(listOf(Position(0, 0), Position(0, 1)))))
        ownBoard.addShot(Position(0, 0))
        val opponentBoard = Board(listOf(Ship(listOf(Position(9, 9)))))
        val session = GameSession(
            "g1",
            Player("p1", "P1"),
            Player("p2", "P2"),
            ownBoard,
            opponentBoard
        )

        val rendering = BoardRenderer.renderForPlayer(session, "p1")
        val ownView = rendering.substringAfter("Ваше поле:\n").substringBefore("\nПоле противника:")
        val opponentView = rendering.substringAfter("Поле противника:\n")

        assertTrue(ownView.contains("0 X"))
        assertTrue(ownView.contains("1 S"))
        assertFalse(opponentView.contains("S"))
    }
}
