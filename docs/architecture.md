# Архитектура Battleship

## Модули

- `core` содержит модели партии, правила, игровой движок, генератор доски, рейтинг и интерфейсы репозиториев. Он не зависит от CLI.
- `cli` принимает команды, вызывает API `core`, отображает поля и журнал. Единственная реализация репозиториев сейчас хранит данные в `MutableMap`.
- `gui` пока не содержит реализации игрового интерфейса.

## Модели

- `Position(x, y)` — координаты клетки.
- `Ship(positions)` хранит клетки корабля и попадания; `isSunk()` проверяет, поражены ли все клетки.
- `Board(ships)` содержит корабли и набор выстрелов, сделанных по этой доске. `isAllShipsSunk()` проверяет победу над флотом.
- `Player(id, name, rating)` хранит игрока и его рейтинг.
- `GameSession(id, player1, player2, board1, board2)` хранит две доски, состояние, текущего игрока, победителя и журнал `MoveRecord(playerId, position, result)`.
- `GameState`: `SETUP`, `IN_PROGRESS`, `FINISHED`.
- `MoveResult`: `MISS`, `HIT`, `KILLED`, `ALREADY_SHOT`, `INVALID_MOVE`, `GAME_OVER`.

## Сервисы и интерфейсы core

- `IGameRulesValidator` и `GameRulesValidatorImpl` проверяют координаты и ходы, форму корабля, допустимость его размещения и полную классическую расстановку из десяти кораблей (`1x4`, `2x3`, `3x2`, `4x1`).
- `GameEngine` координирует запуск и обработку хода. `startGame(session)` валидирует обе доски и переводит сессию из `SETUP` в `IN_PROGRESS` только при успешной проверке. `processMove(session, playerId, pos)` записывает результат каждой попытки в журнал, обновляет попадания и очередь. При победе завершает сессию и применяет рейтинг.
- `IRatingCalculator.calculateRatingChange(winner, loser)` предоставляет величину изменения рейтинга. `FixedRatingCalculator` возвращает 10; движок добавляет эти очки победителю и вычитает их у проигравшего.
- `IPlayerRepository` и `IGameRepository` задают операции `save` и `findById`. `InMemoryPlayerRepository` и `InMemoryGameRepository` реализуют их на `MutableMap`.
- `BoardFactory.createRandomBoard()` генерирует случайную классическую расстановку.

## Поток партии

1. CLI создаёт игроков через `IPlayerRepository`, генерирует обе доски и собирает `GameSession` в состоянии `SETUP`.
2. CLI вызывает `GameEngine.startGame`. Если `IGameRulesValidator.validateBoard` отклоняет любую доску, партия не сохраняется и не начинается. При успехе сессия становится `IN_PROGRESS`.
3. Команда `move` передаётся в `GameEngine.processMove`. Валидатор проверяет состояние, очередь и координаты. Повторный выстрел возвращает `ALREADY_SHOT`; недопустимая попытка возвращает `INVALID_MOVE`.
4. Для новой допустимой клетки движок записывает выстрел. Попадание оставляет ход текущему игроку, промах передаёт очередь. Потопление последнего корабля переводит партию в `FINISHED`, задаёт победителя и обновляет рейтинг.
5. Результат попытки добавляется в `GameSession.moves`. CLI сохраняет сессию через `IGameRepository` и выводит очередь (либо победителя по завершении), доступное игроку представление досок и журнал.

## Диаграмма

```mermaid
classDiagram
    class Position {
        +int x
        +int y
    }
    class MoveResult {
        <<enumeration>>
        MISS
        HIT
        KILLED
        ALREADY_SHOT
        INVALID_MOVE
        GAME_OVER
    }
    class GameState {
        <<enumeration>>
        SETUP
        IN_PROGRESS
        FINISHED
    }
    class MoveRecord {
        +String playerId
        +Position position
        +MoveResult result
    }
    class Ship {
        +List~Position~ positions
        +Set~Position~ hits
        +addHit(pos)
        +isSunk() Boolean
    }
    class Board {
        +List~Ship~ ships
        +Set~Position~ shots
        +addShot(pos)
        +isAllShipsSunk() Boolean
    }
    class Player {
        +String id
        +String name
        +int rating
        +updateRating(newRating)
    }
    class GameSession {
        +String id
        +Player player1
        +Player player2
        +Board board1
        +Board board2
        +GameState state
        +String currentTurnPlayerId
        +String winnerId
        +List~MoveRecord~ moves
        +begin()
        +recordMove(move)
        +switchTurn()
        +finishGame(winner)
    }
    class IGameRulesValidator {
        <<interface>>
        +isValidPosition(pos) Boolean
        +validateMove(session, playerId, pos) Boolean
        +validateBoard(board) Boolean
        +isValidShipShape(ship) Boolean
        +canPlaceShip(board, ship) Boolean
    }
    class GameRulesValidatorImpl
    class IRatingCalculator {
        <<interface>>
        +calculateRatingChange(winner, loser) int
    }
    class FixedRatingCalculator
    class GameEngine {
        +startGame(session) Boolean
        +processMove(session, playerId, pos) MoveResult
    }
    class IPlayerRepository {
        <<interface>>
        +save(player)
        +findById(id) Player?
    }
    class IGameRepository {
        <<interface>>
        +save(session)
        +findById(id) GameSession?
    }
    class InMemoryPlayerRepository
    class InMemoryGameRepository
    class BoardFactory {
        <<object>>
        +createValidClassicalBoard() Board
        +createRandomBoard() Board
    }

    Board "1" *-- "0..*" Ship
    GameSession "1" *-- "2" Board
    GameSession "1" --> "2" Player
    GameSession "1" *-- "0..*" MoveRecord
    IGameRulesValidator <|.. GameRulesValidatorImpl
    IRatingCalculator <|.. FixedRatingCalculator
    IPlayerRepository <|.. InMemoryPlayerRepository
    IGameRepository <|.. InMemoryGameRepository
    GameEngine --> IGameRulesValidator
    GameEngine --> IRatingCalculator
    GameEngine ..> GameSession
    BoardFactory ..> GameRulesValidatorImpl
```
