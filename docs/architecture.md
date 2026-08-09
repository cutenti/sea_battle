# Архитектура проекта Battleship

## 1. Data Classes
* `Position`: Координаты клетки (x, y).
* `ShipType`: Перечисление типов кораблей с указанием их размеров (`CARRIER` - 4-палубник, `BATTLESHIP`, `CRUISER`, `DESTROYER` - 1-палубник).
* `Ship`: Определяет корабль: его тип (`ShipType`), занимаемые позиции (`positions`) и попадания (`hits`). Содержит логику проверки затопления (`isSunk`).
* `Board`: Игровое поле. Хранит только список кораблей (`ships`) и множество координат всех совершенных выстрелов (`shots`). Вычисляет затопление всех кораблей (`isAllShipsSunk`).
* `Player`: Профиль игрока (ID, имя, рейтинг).
* `GameSession`: Текущее состояние партии. Включает игроков, их доски, текущую стадию `GameState` (`SETUP`, `IN_PROGRESS`, `FINISHED`), прямую ссылку на активного игрока (`currentTurnPlayerId`) и ID победителя.
* `GameState`: Перечисление состояний игры (`SETUP`, `IN_PROGRESS`, `FINISHED`).
* `MoveResult`: Результат хода (`MISS`, `HIT`, `KILLED`, `ALREADY_SHOT`, `INVALID_MOVE`, `GAME_OVER`).

## 2. Основные интерфейсы
* `IGameRulesValidator`:
  * `isValidPosition(pos: Position)` — проверка выхода за пределы игрового поля.
  * `validateSetup(board: Board)`: Проверяет правильность начальной расстановки кораблей перед стартом (наличие необходимых типов кораблей `ShipType`, нахождение в пределах сетки, отсутствие касаний по сторонам и диагоналям).
  * `validateMove(session: GameSession, pos: Position)`: Проверяет допустимость хода (игра находится в статусе `IN_PROGRESS`, выстрел производится активным игроком `session.currentTurnPlayerId`, координаты в пределах поля).
* `IGameEngine`:
  * `processMove(session: GameSession, pos: Position)` — центральный метод обработки хода. Вызывает валидатор правил, обновляет состояние поля сессии, переключает ход и обновляет рейтинг через `IRatingCalculator` в случае окончания игры.
* `IRatingCalculator`:
  * `calculateRatingChange(winner: Player, loser: Player)` — расчет изменения рейтинга по завершении игры.
* `InMemoryPlayerRepository` / `InMemoryGameRepository`:
  * Интерфейсы доступа к данным для сохранения и загрузки истории игр и профилей игроков

## 3. Поток исполнения
1. **Ввод:** Пользователь через графический или консольный интерфейс указывает координаты выстрела.
2. **Запрос:** UI вызывает метод `processMove(session, pos)` в сервисе `IGameEngine`.
3. **Валидация:** `IGameEngine` запрашивает у `IGameRulesValidator` проверку стадии игры (`IN_PROGRESS`), очередности хода (`currentTurnPlayerId`) и корректности координат. При ошибке возвращается `INVALID_MOVE`.
4. **Обработка:** При успешной валидации `IGameEngine` проверяет, не было ли выстрела в эту клетку ранее (возвращает `ALREADY_SHOT`). Если клетка новая, координата добавляется в `shots` доски противника.
5. **Изменение состояния:** В зависимости от попадания, `IGameEngine` рассчитывает результат (`HIT`, `KILLED`, `MISS`). При промахе право хода передается противнику. При уничтожении последнего корабля статус игры меняется на `FINISHED`, фиксируется победитель, рассчитывается изменение рейтинга через `IRatingCalculator` и возвращается `GAME_OVER`.
6. **Сохранение и обновление:** `InMemoryGameRepository` сохраняет состояние, а UI обновляется на основе полученного `MoveResult` и текущего состояния `Board`.

## 4. Диаграмма классов

```mermaid
classDiagram
    class Position {
        +int x
        +int y
    }

    class ShipType {
        <<enumeration>>
        CARRIER
        BATTLESHIP
        CRUISER
        DESTROYER
        +int size
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

    class Ship {
        +ShipType type
        +List~Position~ positions
        +Set~Position~ hits
        +isSunk() Boolean
    }

    class Board {
        +List~Ship~ ships
        +Set~Position~ shots
        +isAllShipsSunk() Boolean
    }

    class Player {
        +String id
        +String name
        +int rating
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
    }

    class IGameRulesValidator {
        <<interface>>
        +isValidPosition(pos: Position) Boolean
        +validateSetup(board: Board) Boolean
        +validateMove(session: GameSession, pos: Position) Boolean
    }

    class GameRulesValidatorImpl {
        +isValidPosition(pos: Position) Boolean
        +validateSetup(board: Board) Boolean
        +validateMove(session: GameSession, pos: Position) Boolean
    }

    class IRatingCalculator {
        <<interface>>
        +calculateRatingChange(winner: Player, loser: Player) int
    }

    class EloRatingCalculator {
        +calculateRatingChange(winner: Player, loser: Player) int
    }

    class IGameEngine {
        +processMove(session: GameSession, pos: Position) MoveResult
    }

    class InMemoryPlayerRepository {
        +save(player: Player)
        +findById(id: String) Player
    }

    class InMemoryGameRepository {
        +save(session: GameSession)
        +findById(id: String) GameSession
    }

    Ship "1" --> "1" ShipType
    Board "1" *-- "1..*" Ship
    GameSession "1" *-- "2" Board
    GameSession "1" --> "2" Player
    IGameRulesValidator <|.. GameRulesValidatorImpl
    IRatingCalculator <|.. EloRatingCalculator
    IGameEngine --> IGameRulesValidator
    IGameEngine --> IRatingCalculator
    IGameEngine ..> GameSession