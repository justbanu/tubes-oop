package nangor.core;

import java.util.List;
import java.util.Set;

public record GameSnapshot(GameState state, List<String> board, PlayerView player,
                           List<EnemyView> enemies, List<BombView> bombs,
                           Set<Position> explosions, Set<Position> warning, Set<Position> attack,
                           int score, long elapsedMillis, int secondsLeft, String message,
                           String bossStage, AttackPattern bossAttack, int bossPhase, boolean vulnerable) {
    public GameSnapshot {
        board = List.copyOf(board); enemies = List.copyOf(enemies); bombs = List.copyOf(bombs);
        explosions = Set.copyOf(explosions); warning = Set.copyOf(warning); attack = Set.copyOf(attack);
    }
    public record PlayerView(Position position, int lives, int capacity, int occupiedSlots,
                             int range, double speed, BombType selected, boolean remote, boolean invulnerable) { }
    public record EnemyView(long id, Position position, int health, String kind, String behavior) { }
    public record BombView(Position position, BombType type, int range, boolean armed) { }
}
