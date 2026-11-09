package nangor.core;

final class PatrolStrategy implements MovementStrategy {
    @Override public Position next(NormalEnemy enemy, GameEngine game) {
        Position target = enemy.route.get(enemy.routeIndex);
        if (enemy.position.equals(target)) {
            if (enemy.routeIndex == enemy.route.size() - 1) enemy.routeStep = -1;
            if (enemy.routeIndex == 0) enemy.routeStep = 1;
            enemy.routeIndex += enemy.routeStep;
            target = enemy.route.get(enemy.routeIndex);
        }
        return game.pathStep(enemy.position, target, p -> game.enemyCanEnter(p, enemy));
    }
}
