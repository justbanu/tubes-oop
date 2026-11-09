package nangor.core;

final class RemoteBomb extends Bomb {
    RemoteBomb(long id, Player player) { super(id, player, player.blastRange, BombType.REMOTE); }
    @Override boolean due(long now) { return false; }
}
