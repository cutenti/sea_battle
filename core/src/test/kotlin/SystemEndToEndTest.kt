import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SystemEndToEndTest {

    @Test
    fun `Full game cycle from setup to finish`() {
        // Инициализация системы
        val playerRepo = InMemoryPlayerRepository()
        val gameRepo = InMemoryGameRepository()
        val engine = GameEngine(GameRulesValidatorImpl())

        // 1. Создание игроков
        val p1 = Player("p1", "Alice")
        val p2 = Player("p2", "Bob")
        playerRepo.save(p1)
        playerRepo.save(p2)

        // 2. Создание игры (Один однопалубный корабль у каждого для скорости теста)
        val b1 = Board(listOf(Ship(listOf(Position(5, 5)))))
        val b2 = Board(listOf(Ship(listOf(Position(2, 2)))))
        val session = GameSession("g1", p1, p2, b1, b2)
        gameRepo.save(session)

        // 3. Alice стреляет мимо
        assertEquals(
            MoveResult.MISS, 
            engine.processMove(session, p1.id, Position(0, 0))
        )

        // 4. Bob стреляет мимо
        assertEquals(
            MoveResult.MISS, 
            engine.processMove(session, p2.id, Position(1, 1))
        )

        // 5. Alice стреляет второй раз, попадает и побеждает
        val finalMove = engine.processMove(session, p1.id, Position(2, 2))
        
        // 6. Проверки инвариантности и финала
        assertEquals(MoveResult.GAME_OVER, finalMove)
        assertEquals(GameState.FINISHED, session.state)
        assertEquals(p1.id, session.winnerId)
    }

    @Test
    fun `System handles invalid moves, out of turn shots, and repeated shots gracefully`() {
        val engine = GameEngine(GameRulesValidatorImpl())
        val p1 = Player("p1", "Alice")
        val p2 = Player("p2", "Bob")
        val b1 = Board(listOf(Ship(listOf(Position(0, 0)))))
        val b2 = Board(listOf(Ship(listOf(Position(9, 9)))))
        val session = GameSession("g1", p1, p2, b1, b2)

        // 1. Bob пытается выстрелить первым (не его очередь)
        assertEquals(
            MoveResult.INVALID_MOVE, 
            engine.processMove(session, p2.id, Position(5, 5))
        )

        // 2. Alice стреляет за пределы игрового поля (координаты 10, 10)
        assertEquals(
            MoveResult.INVALID_MOVE, 
            engine.processMove(session, p1.id, Position(10, 10))
        )

        // 3. Alice делает корректный выстрел, но промахивается
        assertEquals(
            MoveResult.MISS, 
            engine.processMove(session, p1.id, Position(5, 5))
        )
        // Проверяем, что ход перешел к Бобу
        assertEquals(p2.id, session.currentTurnPlayerId)

        // 4. Bob стреляет и тоже промахивается
        engine.processMove(session, p2.id, Position(2, 2))
        
        // 5. Ход вернулся к Алисе. Она случайно стреляет в ту же самую клетку (5, 5)
        assertEquals(
            MoveResult.ALREADY_SHOT, 
            engine.processMove(session, p1.id, Position(5, 5))
        )
        // При ALREADY_SHOT ход не передается, Алиса должна переходить
        assertEquals(p1.id, session.currentTurnPlayerId)
    }
}