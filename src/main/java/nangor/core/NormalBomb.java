package nangor.core;

final class NormalBomb extends Bomb {
    private final long explodeAt;
    NormalBomb(long id, Player player, long now) {
        super(id, player, player.blastRange, BombType.NORMAL); explodeAt = now + 3000;
    }
    @Override boolean due(long now) { return now >= explodeAt; }
}
