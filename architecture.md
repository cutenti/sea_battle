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

    class Ship {
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
        +validateMove(session: GameSession, playerId: String, pos: Position) Boolean
    }

    class GameRulesValidatorImpl {
        +isValidPosition(pos: Position) Boolean
        +validateMove(session: GameSession, playerId: String, pos: Position) Boolean
    }

    class GameEngine {
        +processMove(session: GameSession, playerId: String, pos: Position) MoveResult
    }

    class InMemoryPlayerRepository {
        +save(player: Player)
        +findById(id: String) Player
    }

    class InMemoryGameRepository {
        +save(session: GameSession)
        +findById(id: String) GameSession
    }

    Board "1" *-- "1..*" Ship
    GameSession "1" *-- "2" Board
    GameSession "1" --> "2" Player
    IGameRulesValidator <|.. GameRulesValidatorImpl
    GameEngine --> IGameRulesValidator
    GameEngine ..> GameSession