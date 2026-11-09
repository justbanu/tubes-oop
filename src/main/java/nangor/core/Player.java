package nangor.core;

final class Player extends Entity {
    int lives = 3, bombCapacity = 1, blastRange = 1, speedLevel;
    boolean remoteUnlocked;
    Direction direction = Direction.DOWN;
    BombType selected = BombType.NORMAL;
    long invulnerableUntil, nextMove;

    Player(long id, Position position) { super(id, position); }
    long moveInterval() { return 300L * 2 / (2 + speedLevel); }
    boolean invulnerable(long now) { return now < invulnerableUntil; }

    void collect(PowerUpType type) {
        switch (type) {
            case BOMB_UP -> bombCapacity = Math.min(3, bombCapacity + 1);
            case FIRE_UP -> blastRange = Math.min(5, blastRange + 1);
            case SPEED_UP -> speedLevel = Math.min(2, speedLevel + 1);
            case REMOTE -> remoteUnlocked = true;
            case HEART -> lives = Math.min(5, lives + 1);
        }
    }
}
