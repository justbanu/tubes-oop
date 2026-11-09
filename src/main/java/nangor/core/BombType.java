package nangor.core;

/** Factory Method: each concrete type creates a bomb with its own fuse policy. */
public enum BombType {
    NORMAL('o', 1) {
        @Override Bomb create(long id, Player player, long now) { return new NormalBomb(id, player, now); }
    },
    PUMPED('O', 2) {
        @Override Bomb create(long id, Player player, long now) { return new PumpedBomb(id, player, now); }
    },
    REMOTE('@', 1) {
        @Override Bomb create(long id, Player player, long now) { return new RemoteBomb(id, player); }
    };
    public final char symbol;
    public final int damage;
    BombType(char symbol, int damage) { this.symbol = symbol; this.damage = damage; }
    abstract Bomb create(long id, Player player, long now);
}
