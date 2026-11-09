package nangor.core;

abstract class Bomb extends Entity {
    final long owner;
    final int range;
    final BombType type;
    boolean armed = true;
    long releaseAt = Long.MAX_VALUE;

    Bomb(long id, Player player, int range, BombType type) {
        super(id, player.position); owner = player.id; this.range = range; this.type = type;
    }
    abstract boolean due(long now);
}
