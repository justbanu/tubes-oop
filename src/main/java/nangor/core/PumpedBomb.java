package nangor.core;

final class PumpedBomb extends Bomb {
    private final long explodeAt;
    PumpedBomb(long id, Player player, long now) {
        super(id, player, Math.min(7, player.blastRange + 2), BombType.PUMPED); explodeAt = now + 4500;
    }
    @Override boolean due(long now) { return now >= explodeAt; }
}
