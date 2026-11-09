package nangor.core;

import java.nio.file.Path;
import java.util.Random;

final class TestSupport {
    private TestSupport() { }
    static final class Clock implements NanoClock {
        long nanos;
        @Override public long nanoTime() { return nanos; }
    }
    static final class Rig {
        final Clock clock = new Clock();
        final GameEngine game;
        Rig() throws Exception { this(17); }
        Rig(long seed) throws Exception { game = new GameEngine(MapDefinition.load(Path.of("config")), new Random(seed), clock); }
        void start() { game.accept(GameCommand.Action.START); }
        void advance(long millis) { clock.nanos += millis * 1_000_000; game.pulse(); }
        void command(GameCommand command) { game.accept(command); }
        void move(Direction direction) { command(new GameCommand.Move(direction)); }
        void isolate() { for (Enemy e : game.enemies.all()) e.health = 0; }
    }
    static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
    static void equal(Object actual, Object expected, String description) {
        if (!actual.equals(expected)) throw new AssertionError(description + ": expected=" + expected + ", actual=" + actual);
    }
}
