package nangor.core;

abstract class Enemy extends Entity {
    int health;
    final int scoreValue;
    Enemy(long id, Position position, int health, int scoreValue) {
        super(id, position); this.health = health; this.scoreValue = scoreValue;
    }
    boolean alive() { return health > 0; }
    abstract void update(GameEngine game);
}
