package nangor.core;

interface MovementStrategy {
    Position next(NormalEnemy enemy, GameEngine game);
}
