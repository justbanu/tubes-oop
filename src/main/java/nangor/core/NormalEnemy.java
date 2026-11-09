package nangor.core;

import java.util.List;

final class NormalEnemy extends Enemy {
    final List<Position> route;
    final MovementStrategy movement;
    int routeIndex, routeStep = 1;
    boolean chasing;
    long nextMove = 700;

    NormalEnemy(long id, MapDefinition.EnemySpec spec) {
        super(id, spec.route().get(0), 1, 100); route = spec.route();
        movement = spec.chase() ? new ChaseStrategy() : new PatrolStrategy();
    }

    @Override void update(GameEngine game) {
        if (!alive() || game.now < nextMove) return;
        nextMove = game.now + 700;
        Position next = movement.next(this, game);
        if (next.equals(game.player.position)) game.hurtPlayer();
        else if (game.enemyCanEnter(next, this)) position = next;
    }
}
