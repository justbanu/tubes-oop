package nangor.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Draco extends Enemy {
    enum Stage { WAITING, WARNING, ACTIVE, COOLDOWN }
    Stage stage = Stage.WAITING;
    AttackPattern pattern, previous;
    Direction direction = Direction.LEFT;
    Set<Position> area = Set.of();
    List<Position> swoopPath = List.of();
    Position target;
    long transitionAt, activeStart, activeDuration, vulnerableUntil;
    boolean hitPlayer;
    int swoopSteps;

    Draco(long id, Position spawn) { super(id, spawn, 8, 500); }
    int phase() { return health <= 4 ? 2 : 1; }
    boolean vulnerable(long now) { return alive() && stage == Stage.COOLDOWN && now < vulnerableUntil; }

    @Override void update(GameEngine game) {
        if (!alive()) return;
        // The arena has no physical gate: attacks activate when Deeper enters its bounds.
        if (stage == Stage.WAITING) {
            if (game.definition.arena.contains(game.player.position)) begin(game);
            return;
        }
        if (game.now >= transitionAt) {
            switch (stage) {
                case WARNING -> {
                    stage = Stage.ACTIVE; activeStart = game.now; transitionAt = game.now + activeDuration;
                }
                case ACTIVE -> {
                    moveSwoop(game, true);
                    stage = Stage.COOLDOWN; transitionAt = game.now + 5000;
                    vulnerableUntil = pattern.opensVulnerability() ? game.now + 4000 : game.now;
                    area = Set.of();
                }
                case COOLDOWN -> begin(game);
                default -> { }
            }
        }
        if (stage == Stage.ACTIVE) {
            moveSwoop(game, false);
            if (pattern != AttackPattern.SWOOP && area.contains(game.player.position) && !hitPlayer && game.hurtPlayer()) hitPlayer = true;
        }
    }

    List<AttackPattern> eligible(Position player) {
        List<AttackPattern> choices = new ArrayList<>();
        boolean aligned = position.x() == player.x() || position.y() == player.y();
        if (aligned && !position.equals(player)) {
            choices.add(AttackPattern.FIRE_BREATH); choices.add(AttackPattern.SWOOP);
            if (position.distance(player) <= (phase() == 2 ? 2 : 1)) choices.add(AttackPattern.BITE);
        }
        choices.add(AttackPattern.WING_GUST);
        if (choices.size() > 1) choices.remove(previous);
        return choices;
    }

    void begin(GameEngine game) {
        List<AttackPattern> choices = eligible(game.player.position);
        pattern = choices.get(game.random.nextInt(choices.size())); previous = pattern;
        target = game.player.position; direction = Direction.toward(position, target);
        stage = Stage.WARNING; transitionAt = game.now + 1000;
        activeDuration = pattern.duration(phase()); hitPlayer = false; swoopSteps = 0;
        Set<Position> tiles = new LinkedHashSet<>();
        List<Position> path = new ArrayList<>();
        if (pattern == AttackPattern.WING_GUST) {
            int radius = phase() == 2 ? 2 : 1;
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
                Position p = new Position(position.x() + x, position.y() + y);
                if ((x != 0 || y != 0) && MapDefinition.inside(p) && !game.blocks.containsKey(p)) tiles.add(p);
            }
        } else {
            int range = switch (pattern) {
                case BITE -> phase() == 2 ? 2 : 1;
                case SWOOP -> position.distance(target);
                default -> MapDefinition.WIDTH;
            };
            Position p = position;
            for (int i = 0; i < range; i++) {
                p = p.step(direction);
                if (!MapDefinition.inside(p) || game.blocks.containsKey(p)) break;
                if (pattern == AttackPattern.SWOOP && !game.bossCanEnter(p)) break;
                tiles.add(p); path.add(p);
            }
        }
        area = Set.copyOf(tiles); swoopPath = List.copyOf(path);
        game.message = "Draco: " + pattern + "! Hindari tile !";
    }

    private void moveSwoop(GameEngine game, boolean finish) {
        if (pattern != AttackPattern.SWOOP) return;
        int steps = finish ? swoopPath.size()
                : (int) ((game.now - activeStart) * swoopPath.size() / activeDuration);
        while (swoopSteps < steps) {
            Position p = swoopPath.get(swoopSteps++);
            if (!game.bossCanEnter(p)) { swoopSteps = swoopPath.size(); break; }
            if (p.equals(game.player.position) && !hitPlayer && game.hurtPlayer()) hitPlayer = true;
            // A protected player remains a solid actor.
            if (p.equals(game.player.position)) { swoopSteps = swoopPath.size(); break; }
            position = p;
        }
    }
}
