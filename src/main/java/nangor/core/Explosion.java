package nangor.core;

import java.util.HashSet;
import java.util.Set;

final class Explosion extends Entity {
    final Set<Position> tiles;
    final Set<Long> damaged = new HashSet<>();
    final BombType type;
    final long endAt;
    Explosion(long id, Bomb bomb, Set<Position> tiles, long now) {
        super(id, bomb.position); this.tiles = Set.copyOf(tiles); type = bomb.type; endAt = now + 500;
    }
}
