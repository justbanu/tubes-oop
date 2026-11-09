package nangor.core;

final class ChaseStrategy implements MovementStrategy {
    private final MovementStrategy patrol = new PatrolStrategy();
    @Override public Position next(NormalEnemy enemy, GameEngine game) {
        enemy.chasing = enemy.position.distance(game.player.position) <= 3;
        return enemy.chasing
                ? game.pathStep(enemy.position, game.player.position, p -> game.enemyCanEnter(p, enemy))
                : patrol.next(enemy, game);
    }
}
