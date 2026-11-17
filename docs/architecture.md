# Архитектура Battleship

## Модули

- `core` содержит модели партии, правила, игровой сервис и движок, генератор доски, рейтинг и интерфейсы репозиториев. Он не зависит от UI.
- `cli` принимает команды, вызывает API `core` и отображает поля и журнал.
- `gui` содержит Swing-приложение, контроллер игрового экрана и CSV-реализацию реестра игроков.

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
- `GameService` реализует сценарии добавления и поиска игроков, создания проверенной партии и записи хода. При старте он проверяет обе доски и переводит сессию из `SETUP` в `IN_PROGRESS`; запись результата в журнал и сохранение партии также выполняются сервисом.
- `GameEngine.processMove(session, playerId, pos)` проверяет выстрел, обновляет попадания, очередь, состояние партии и рейтинг при победе.
- `IRatingCalculator.calculateRatingChange(winner, loser)` предоставляет величину изменения рейтинга. GUI и CLI используют `EloRatingCalculator` с K=32; `FixedRatingCalculator` оставлен как простая фиксированная политика.
- `IPlayerRepository` и `IGameRepository` задают операции `save`, `findById` и `findAll`. `InMemoryPlayerRepository` и `InMemoryGameRepository` реализуют их на `MutableMap`.
- `BoardFactory.createRandomBoard()` генерирует случайную классическую расстановку.
- `GameController` связывает UI с `GameService`, управляет текущей партией, таймером и локальными статистиками.
- `BattleshipFrame` отрисовывает оба поля, счёт и историю, позволяет добавлять игроков/запускать партию/делать выстрелы и сворачивать таблицу игроков.
- `CsvPlayerRepository` реализует `IPlayerRepository`, хранит ID, имя и рейтинг игроков в UTF-8 CSV с атомарной заменой файла. Приложение GUI использует `~/.sea-battle/players.csv`; ошибка чтения или записи не подменяется пустым реестром.
- Таблица статистики считает игры, победы и поражения по сессиям текущего запуска. Рейтинг сохраняется в CSV, а игровая история пока остаётся в памяти.

GUI запускается командой `./gradlew :gui:run`.

## Поток партии

1. CLI или GUI создаёт игроков через `IPlayerRepository`, генерирует обе доски и передаёт их в `GameService.startGame`.
2. `GameService` проверяет обе расстановки через `IGameRulesValidator`; невалидные доски не запускают партию. При успехе сессия переходит из `SETUP` в `IN_PROGRESS` и сохраняется через `IGameRepository`.
3. UI передаёт выстрел сервису. Сервис вызывает `GameEngine.processMove`, заносит результат (в том числе отклонённую попытку) в `GameSession.moves` и сохраняет сессию.
4. Движок проверяет состояние, очередь и координаты. Повторный выстрел возвращает `ALREADY_SHOT`; попадание оставляет ход текущему игроку, промах передаёт очередь. Потопление последнего корабля завершает партию и обновляет рейтинг.
5. GUI отображает поля, счёт попаданий, таймер, журнал и статистику игроков. `CsvPlayerRepository` сохраняет имена и рейтинг на диск; сами партии и промежуточные победы/поражения хранятся в памяти текущего запуска.

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
        +addMove(move)
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
    class EloRatingCalculator
    class IGameEngine {
        <<interface>>
        +processMove(session, playerId, pos) MoveResult
    }
    class GameEngine {
        +processMove(session, playerId, pos) MoveResult
    }
    class GameService {
        +addPlayer(name) Player
        +findPlayer(id) Player?
        +listPlayers() List~Player~
        +findGame(id) GameSession?
        +listGames() List~GameSession~
        +startGame(player1, player2, board1, board2) GameSession
        +recordMove(session, playerId, pos) MoveResult
    }
    class IPlayerRepository {
        <<interface>>
        +save(player)
        +findById(id) Player?
        +findAll() List~Player~
    }
    class IGameRepository {
        <<interface>>
        +save(session)
        +findById(id) GameSession?
        +findAll() List~GameSession~
    }
    class InMemoryPlayerRepository
    class InMemoryGameRepository
    class CsvPlayerRepository
    class BattleshipFrame
    class BoardFactory {
        <<object>>
        +createValidClassicalBoard() Board
        +createRandomBoard(validator, random, maxAttempts) Board
    }
    class GameController {
        +players() List~Player~
        +addPlayer(name) Player
        +startGame(firstPlayerId, secondPlayerId) GameSession
        +shoot(position) MoveResult
        +elapsedSeconds() long
        +statistics() List~PlayerStatistics~
    }
    class PlayerStatistics {
        +Player player
        +int gamesPlayed
        +int wins
        +int losses
    }
    class BattleshipFrame

    Board "1" *-- "0..*" Ship
    GameSession "1" *-- "2" Board
    GameSession "1" --> "2" Player
    GameSession "1" *-- "0..*" MoveRecord
    IGameRulesValidator <|.. GameRulesValidatorImpl
    IRatingCalculator <|.. FixedRatingCalculator
    IRatingCalculator <|.. EloRatingCalculator
    IGameEngine <|.. GameEngine
    IPlayerRepository <|.. InMemoryPlayerRepository
    IPlayerRepository <|.. CsvPlayerRepository
    IGameRepository <|.. InMemoryGameRepository
    GameService --> IGameRepository
    GameService --> IPlayerRepository
    GameService --> IGameEngine
    GameService --> IGameRulesValidator
    GameEngine --> IGameRulesValidator
    GameEngine --> IRatingCalculator
    GameEngine ..> GameSession
    BoardFactory ..> GameRulesValidatorImpl
    GameController --> GameService
    BattleshipFrame --> GameController
```
