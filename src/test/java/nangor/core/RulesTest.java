package nangor.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import nangor.runtime.CommandQueue;
import nangor.runtime.KeyBindings;
import nangor.runtime.KeyboardReader;
import static nangor.core.TestSupport.*;

public final class RulesTest {
    @FunctionalInterface private interface Test { void run() throws Exception; }
    private static int passed;
    private RulesTest() { }
    private static void test(String name, Test test) throws Exception {
        test.run(); passed++; System.out.println("PASS " + name);
    }

    public static void main(String[] args) throws Exception {
        test("F00: exact map, entities and initial values", RulesTest::initial);
        test("F00: map size, symbols, spawns, routes and reachability validation", RulesTest::validation);
        test("F01: movement, collision and movement cooldown", RulesTest::movement);
        test("F01: respawn, nearest free tile and 1500ms invulnerability", RulesTest::respawn);
        test("F02: normal fuse, 500ms flame and delayed capacity return", RulesTest::normalFuse);
        test("F02: pumped fuse, range cap and immutable placed type", RulesTest::pumped);
        test("F02: remote lock, no fuse and detonate all", RulesTest::remote);
        test("F02-F03: bounded chain, one explosion per bomb, leave/no re-entry", RulesTest::chain);
        test("F03-F04: block propagation, excluded destroyed tile and reinforced", RulesTest::blocks);
        test("F03: one hit per enemy per activation, distinct overlapping blasts", RulesTest::damageOnce);
        test("F05: patrol and chase at Manhattan distance three", RulesTest::enemyStrategies);
        test("F05: boss warning, fixed target, cooldown and vulnerability boundaries", RulesTest::bossTiming);
        test("F05: no repeated eligible attack and phase two shapes/durations", RulesTest::bossPatterns);
        test("F05: swoop path respects blocks, bomb, arena and contact", RulesTest::swoop);
        test("F06: all power-ups, caps, persistence and restart", RulesTest::powerUps);
        test("F06: 30 percent boundary and all five random drops", RulesTest::drops);
        test("F08-F09: win gate, score once, no loss bonus, timeout precedence", RulesTest::endStates);
        test("F09: pause freezes bomb/enemy/boss/invulnerability/game time", RulesTest::pause);
        test("F09: restart resets every mutable subsystem", RulesTest::restart);
        test("Concurrency: owner guard, immutable snapshot and bounded queue", RulesTest::concurrency);
        test("Input: raw characters, arrow decoding and state bindings", RulesTest::input);
        System.out.println("RulesTest: " + passed + "/" + passed + " groups passed");
    }

    private static Rig playing() throws Exception { Rig r = new Rig(); r.start(); return r; }
    private static Rig isolated() throws Exception { Rig r = playing(); r.isolate(); return r; }
    private static Properties properties() throws Exception {
        Properties p = new Properties(); try (var in = Files.newInputStream(Path.of("config/green-garden.properties"))) { p.load(in); } return p;
    }
    private static void rejects(List<String> rows, Properties p) throws Exception {
        try { MapDefinition.parse(rows, p); throw new AssertionError("Invalid map accepted"); }
        catch (MapValidationException expected) { check(!expected.getMessage().isBlank(), "Actionable validation message"); }
    }

    private static void initial() throws Exception {
        Rig r = playing(); GameEngine g = r.game;
        equal(g.definition.rows, List.of("##############", "#P..D...D...X#", "#.#.###.#.D..#", "#.#...#..E##.#", "#D..R...R....#",
                "###.###.###E.#", "#.E.D...#....#", "#.###.#...B..#", "#E....D.....E#", "##############"), "PDF layout");
        equal(g.enemies.size(), 6, "five normals and Draco"); equal(g.player.lives, 3, "lives");
        equal(g.player.bombCapacity, 1, "capacity"); equal(g.player.blastRange, 1, "range");
        equal(g.player.selected, BombType.NORMAL, "default bomb"); equal(g.score, 0, "score"); equal(g.secondsLeft(), 300, "timer");
        check(g.bombs.size() == 0 && g.explosions.size() == 0, "empty bombs and flames");
    }
    private static void validation() throws Exception {
        List<String> rows = Files.readAllLines(Path.of("config/green-garden.map"));
        rejects(rows.subList(0, 9), properties());
        List<String> wrong = new ArrayList<>(rows); wrong.set(1, wrong.get(1).replace('P', '.')); rejects(wrong, properties());
        wrong = new ArrayList<>(rows); wrong.set(1, wrong.get(1).replace('X', 'G')); rejects(wrong, properties());
        wrong = new ArrayList<>(rows); wrong.set(0, ".#############"); rejects(wrong, properties());
        wrong = new ArrayList<>(rows); wrong.set(1, "#P#.D...D...X#"); wrong.set(2, "###.###.#.D..#"); rejects(wrong, properties());
        Properties p = properties(); p.setProperty("remote", "0,0"); rejects(rows, p);
        p = properties(); p.setProperty("enemy.0", "PATROL;9,3;7,3"); rejects(rows, p);
        p = properties(); p.setProperty("arena", "0,0,3,3"); rejects(rows, p);
        p = properties(); p.setProperty("enemy.0", "ALIEN;9,3;8,3"); rejects(rows, p);
        try { MapDefinition.load(Path.of("missing-map")); throw new AssertionError("Missing config accepted"); }
        catch (MapValidationException expected) { check(expected.getCause() != null, "I/O cause preserved"); }
    }
    private static void movement() throws Exception {
        Rig r = isolated(); r.move(Direction.UP); equal(r.game.player.position, new Position(1, 1), "wall collision");
        r.move(Direction.RIGHT); equal(r.game.player.position, new Position(2, 1), "move");
        r.move(Direction.RIGHT); equal(r.game.player.position, new Position(2, 1), "rate limit");
        r.advance(300); r.move(Direction.RIGHT); r.advance(300); r.move(Direction.RIGHT);
        equal(r.game.player.position, new Position(3, 1), "destructible collision");
        NormalEnemy e = (NormalEnemy) r.game.enemies.all().get(0); e.health = 1; e.position = new Position(3, 2);
        r.move(Direction.DOWN); equal(r.game.player.lives, 2, "contact damage");
        check(!r.game.player.position.equals(e.position), "no actor overlap");
    }
    private static void respawn() throws Exception {
        Rig r = isolated(); GameEngine g = r.game;
        r.command(GameCommand.Action.PLACE); r.move(Direction.RIGHT);
        check(g.hurtPlayer(), "damage applies"); equal(g.player.position, new Position(2, 1), "nearest free spawn (bomb blocks origin)");
        equal(g.player.lives, 2, "one life lost"); check(!g.hurtPlayer(), "invulnerable");
        r.advance(1490); check(!g.hurtPlayer(), "invulnerability lasts 1490ms");
        r.advance(10); check(g.hurtPlayer(), "expires at 1500ms");
        r.advance(1500); g.hurtPlayer(); equal(g.player.lives, 0, "zero lives");
    }
    private static void normalFuse() throws Exception {
        Rig r = isolated(); r.command(GameCommand.Action.PLACE); r.move(Direction.RIGHT); r.advance(300); r.move(Direction.RIGHT);
        r.advance(2690); equal(r.game.explosions.size(), 0, "before fuse");
        r.advance(10); equal(r.game.explosions.size(), 1, "at 3000"); equal(r.game.bombs.size(), 1, "slot held during flame");
        r.command(GameCommand.Action.PLACE); equal(r.game.bombs.size(), 1, "capacity full");
        r.advance(490); equal(r.game.explosions.size(), 1, "flame 490");
        r.advance(10); equal(r.game.bombs.size(), 0, "slot freed at 3500"); equal(r.game.explosions.size(), 0, "flame expired");
    }
    private static void pumped() throws Exception {
        Rig r = isolated(); GameEngine g = r.game; g.player.blastRange = 5;
        r.command(new GameCommand.Select(BombType.PUMPED)); r.command(GameCommand.Action.PLACE);
        r.command(new GameCommand.Select(BombType.NORMAL)); equal(g.bombs.all().get(0).type, BombType.PUMPED, "placed bomb immutable");
        equal(g.bombs.all().get(0).range, 7, "pumped range cap");
        g.player.position = new Position(5, 3); r.advance(4490); equal(g.explosions.size(), 0, "pumped before fuse");
        r.advance(10); equal(g.explosions.size(), 1, "pumped at 4500");
    }
    private static void remote() throws Exception {
        Rig r = isolated(); r.command(new GameCommand.Select(BombType.REMOTE)); equal(r.game.player.selected, BombType.NORMAL, "remote locked");
        r.move(Direction.RIGHT); check(r.game.player.remoteUnlocked, "guaranteed remote collected");
        r.command(new GameCommand.Select(BombType.REMOTE)); r.game.player.bombCapacity = 3;
        r.command(GameCommand.Action.PLACE); r.advance(300); r.move(Direction.RIGHT); r.command(GameCommand.Action.PLACE);
        r.game.player.position = new Position(5, 3); r.advance(20_000); equal(r.game.explosions.size(), 0, "no remote timer");
        r.command(GameCommand.Action.DETONATE); equal(r.game.explosions.size(), 2, "all remotes detonated once");
        r.command(GameCommand.Action.DETONATE); equal(r.game.explosions.size(), 2, "no repeat");
    }
    private static void chain() throws Exception {
        Rig r = isolated(); GameEngine g = r.game; g.player.bombCapacity = 3;
        r.command(GameCommand.Action.PLACE); r.move(Direction.RIGHT); r.advance(300); r.move(Direction.LEFT);
        equal(g.player.position, new Position(2, 1), "cannot re-enter own bomb");
        r.command(new GameCommand.Select(BombType.REMOTE)); r.command(GameCommand.Action.PLACE);
        r.move(Direction.RIGHT); r.advance(300); r.move(Direction.RIGHT);
        g.player.position = new Position(5, 3); r.advance(2400);
        equal(g.explosions.size(), 2, "normal chains remote"); check(g.bombs.all().stream().noneMatch(b -> b.armed), "all disarmed");
        r.advance(100); equal(g.explosions.size(), 2, "no duplicate activation");
    }
    private static void blocks() throws Exception {
        Rig r = isolated(); GameEngine g = r.game;
        g.player.position = new Position(3, 1); g.player.blastRange = 5;
        Bomb b = BombType.NORMAL.create(100, g.player, g.now); g.bombs.add(b); g.detonate(List.of(b));
        Explosion e = g.explosions.all().get(0);
        check(!g.blocks.containsKey(new Position(4, 1)), "D destroyed");
        check(!e.tiles.contains(new Position(4, 1)) && !e.tiles.contains(new Position(5, 1)), "D tile excluded and stops ray");
        check(!e.tiles.contains(new Position(3, 0)), "wall excluded");
        for (BombType type : BombType.values()) {
            Rig q = isolated(); GameEngine world = q.game; world.player.position = new Position(3, 4);
            Bomb bomb = type.create(100, world.player, 0); world.bombs.add(bomb); world.detonate(List.of(bomb));
            equal(world.blocks.containsKey(new Position(4, 4)), type != BombType.PUMPED, "reinforced " + type);
            check(!world.explosions.all().get(0).tiles.contains(new Position(4, 4)), "reinforced tile excluded");
            check(!world.explosions.all().get(0).tiles.contains(new Position(5, 4)), "reinforced stops ray");
        }
    }
    private static void damageOnce() throws Exception {
        Rig r = isolated(); GameEngine g = r.game; g.boss.health = 8;
        g.boss.stage = Draco.Stage.COOLDOWN; g.boss.transitionAt = 5000; g.boss.vulnerableUntil = 4000;
        g.player.position = new Position(9, 7); g.player.invulnerableUntil = 10_000;
        Bomb b = BombType.PUMPED.create(100, g.player, 0); g.bombs.add(b); g.detonate(List.of(b));
        r.advance(10); equal(g.boss.health, 6, "pumped two damage");
        r.advance(400); equal(g.boss.health, 6, "only once per activation");
        Bomb b2 = BombType.NORMAL.create(101, g.player, g.now); g.bombs.add(b2); g.detonate(List.of(b2));
        r.advance(10); equal(g.boss.health, 5, "separate overlapping activation");
        check(g.player.lives == 3, "all sources respect invulnerability");
    }
    private static void enemyStrategies() throws Exception {
        Rig r = playing(); NormalEnemy chase = (NormalEnemy) r.game.enemies.all().get(0);
        r.game.player.position = new Position(7, 2); r.advance(700); check(chase.chasing, "distance3 chase");
        equal(chase.position, new Position(8, 3), "BFS avoids the wall at 8,2");
        r.game.player.position = new Position(1, 1); r.advance(700); check(!chase.chasing, "returns patrol outside3");
        NormalEnemy patrol = (NormalEnemy) r.game.enemies.all().get(1);
        check(patrol.route.contains(patrol.position), "fixed patrol route");
        for (int i = 0; i < 10; i++) { r.advance(700); check(patrol.route.contains(patrol.position), "patrol remains on route"); }
    }
    private static void bossTiming() throws Exception {
        Rig r = isolated(); GameEngine g = r.game; g.boss.health = 8;
        g.player.position = new Position(8, 7); g.player.invulnerableUntil = 100_000;
        g.boss.previous = AttackPattern.WING_GUST; r.advance(10);
        check(g.boss.pattern == AttackPattern.FIRE_BREATH || g.boss.pattern == AttackPattern.SWOOP, "eligible vulnerability attack");
        Set<Position> area = g.boss.area; Position target = g.boss.target;
        g.player.position = new Position(7, 6); r.advance(990); equal(g.boss.stage, Draco.Stage.WARNING, "full warning second");
        equal(g.boss.target, target, "target locked"); equal(g.boss.area, area, "area locked");
        r.advance(10); equal(g.boss.stage, Draco.Stage.ACTIVE, "warning ends"); check(!g.boss.vulnerable(g.now), "not vulnerable during attack");
        r.advance(1000); equal(g.boss.stage, Draco.Stage.COOLDOWN, "cooldown"); check(g.boss.vulnerable(g.now), "vulnerable after attack");
        r.advance(3990); check(g.boss.vulnerable(g.now), "vulnerable 3990");
        r.advance(10); check(!g.boss.vulnerable(g.now), "not vulnerable at4000");
        r.advance(990); equal(g.boss.stage, Draco.Stage.COOLDOWN, "cooldown 4990");
        r.advance(10); equal(g.boss.stage, Draco.Stage.WARNING, "new warning after5000");
    }
    private static void bossPatterns() throws Exception {
        Rig r = isolated(); GameEngine g = r.game; Draco b = g.boss; b.health = 4;
        equal(b.phase(), 2, "phase2 at half");
        equal(AttackPattern.FIRE_BREATH.duration(2), 500L, "phase2 fire"); equal(AttackPattern.SWOOP.duration(2), 500L, "phase2 swoop");
        equal(AttackPattern.BITE.duration(2), 750L, "bite duration"); equal(AttackPattern.WING_GUST.duration(2), 1000L, "gust duration");
        b.previous = AttackPattern.SWOOP;
        check(!b.eligible(new Position(8, 7)).contains(AttackPattern.SWOOP), "no repeat with alternatives");
        check(b.eligible(new Position(8, 7)).contains(AttackPattern.BITE), "phase2 bite range2");
        b.health = 8; check(!b.eligible(new Position(8, 7)).contains(AttackPattern.BITE), "phase1 bite range1");
        b.health = 4; g.player.position = new Position(9, 8); b.previous = null; b.begin(g);
        equal(b.pattern, AttackPattern.WING_GUST, "diagonal selects gust");
        check(b.area.contains(new Position(12, 7)), "gust radius2"); check(!b.area.contains(b.position), "gust excludes boss");
        check(b.area.stream().noneMatch(g.blocks::containsKey), "boss never attacks through occupied block tile");
        check(!AttackPattern.BITE.opensVulnerability() && !AttackPattern.WING_GUST.opensVulnerability(), "no vulnerability after bite/gust");
    }
    private static void swoop() throws Exception {
        Rig r = isolated(); GameEngine g = r.game; Draco b = g.boss; b.health = 8;
        b.pattern = AttackPattern.SWOOP; b.stage = Draco.Stage.ACTIVE; b.activeStart = 0; b.activeDuration = 1000;
        b.transitionAt = 1000; b.swoopPath = List.of(new Position(9, 7), new Position(8, 7), new Position(7, 7));
        b.area = Set.copyOf(b.swoopPath); g.player.position = new Position(7, 6);
        g.player.position = new Position(8, 7); Bomb bomb = BombType.REMOTE.create(100, g.player, 0); g.bombs.add(bomb);
        g.player.position = new Position(1, 1); r.advance(1000); equal(b.position, new Position(9, 7), "swoop stops at bomb");
        check(!g.bossCanEnter(new Position(6, 7)), "arena collision"); check(!g.bossCanEnter(new Position(8, 6)), "block collision");
        Rig q = isolated(); q.game.boss.health = 8; q.game.boss.stage = Draco.Stage.ACTIVE; q.game.boss.pattern = AttackPattern.WING_GUST;
        q.game.boss.transitionAt = 3000; q.game.boss.area = Set.of(q.game.definition.spawn);
        q.advance(10); equal(q.game.player.lives, 2, "boss damages once");
        q.advance(2000); equal(q.game.player.lives, 2, "same activation cannot redamage after invulnerability expires");
    }
    private static void powerUps() throws Exception {
        Rig r = isolated(); Player p = r.game.player;
        for (PowerUpType t : PowerUpType.values()) for (int i = 0; i < 10; i++) p.collect(t);
        equal(p.bombCapacity, 3, "bomb cap"); equal(p.blastRange, 5, "range cap"); equal(p.lives, 5, "life cap");
        equal(p.speedLevel, 2, "speed cap"); equal(p.moveInterval(), 150L, "speed +100%"); check(p.remoteUnlocked, "remote");
        r.game.hurtPlayer(); equal(p.bombCapacity, 3, "upgrades survive damage");
        r.command(GameCommand.Action.PAUSE); r.command(GameCommand.Action.RESTART);
        equal(r.game.player.speedLevel, 0, "restart speed"); check(!r.game.player.remoteUnlocked, "restart remote");
        r.move(Direction.RIGHT); check(!r.game.powerUps.containsKey(new Position(2, 1)), "collected item removed");
        r.game.powerUps.put(new Position(3, 1), PowerUpType.HEART);
        r.command(new GameCommand.Select(BombType.REMOTE));
        r.command(GameCommand.Action.PLACE); r.command(GameCommand.Action.DETONATE);
        check(r.game.explosions.all().get(0).tiles.contains(new Position(3, 1)), "flame crosses item");
        check(r.game.powerUps.containsKey(new Position(3, 1)), "power-up survives actual blast");
    }
    private static final class DropRandom extends java.util.Random {
        private static final long serialVersionUID = 1L;
        private final double roll; private final int selection;
        DropRandom(double roll, int selection) { this.roll = roll; this.selection = selection; }
        @Override public double nextDouble() { return roll; }
        @Override public int nextInt(int bound) { return selection % bound; }
    }
    private static void drops() throws Exception {
        for (int i = 0; i < 5; i++) for (double roll : new double[]{0.299999, 0.30}) {
            Clock c = new Clock(); GameEngine g = new GameEngine(MapDefinition.load(Path.of("config")), new DropRandom(roll, i), c);
            g.accept(GameCommand.Action.START); g.player.position = new Position(3, 1);
            Bomb b = BombType.NORMAL.create(100, g.player, 0); g.bombs.add(b); g.detonate(List.of(b));
            Position tile = new Position(4, 1);
            equal(g.powerUps.containsKey(tile), roll < 0.3, "strict30% threshold");
            if (roll < 0.3) equal(g.powerUps.get(tile), PowerUpType.values()[i], "each item can drop");
        }
    }
    private static void endStates() throws Exception {
        Rig r = playing(); r.game.player.position = r.game.definition.exit; r.advance(10);
        equal(r.game.state, GameState.PLAYING, "exit locked until every enemy defeated");
        r.isolate(); r.game.score = 1000; r.advance(990);
        equal(r.game.state, GameState.WIN, "exit + all defeated");
        equal(r.game.score, 4290, "1000 +3*100 +299*10");
        r.advance(3000); r.command(GameCommand.Action.PLACE); equal(r.game.score, 4290, "bonus once");
        Rig q = isolated(); q.game.score = 100; q.advance(300_000);
        equal(q.game.state, GameState.LOSE, "timeout"); equal(q.game.score, 100, "no loss bonus");
        Rig z = isolated(); z.game.now = 299_990; z.game.player.position = z.game.definition.exit; z.advance(10);
        equal(z.game.state, GameState.LOSE, "timeout wins tie");
    }
    private static void pause() throws Exception {
        Rig r = playing(); GameEngine g = r.game; r.command(GameCommand.Action.PLACE);
        g.player.invulnerableUntil = 1500; r.advance(500); r.command(GameCommand.Action.PAUSE);
        GameSnapshot before = g.snapshot(); r.advance(60_000); GameSnapshot after = g.snapshot();
        equal(after.elapsedMillis(), before.elapsedMillis(), "game time frozen"); equal(after.board(), before.board(), "entities frozen");
        equal(after.bombs(), before.bombs(), "bomb frozen"); equal(g.player.invulnerableUntil, 1500L, "invuln deadline frozen");
        r.command(new GameCommand.Move(Direction.DOWN)); equal(g.snapshot().board(), before.board(), "ignore gameplay while paused");
        r.command(GameCommand.Action.PAUSE); r.advance(2490); equal(g.explosions.size(), 0, "fuse resumes remaining time");
        r.advance(10); equal(g.explosions.size(), 1, "resumed fuse");
        Rig q = isolated(); q.game.boss.health = 8; q.game.player.position = new Position(8, 7); q.advance(10);
        q.command(GameCommand.Action.PAUSE); q.advance(90_000); equal(q.game.boss.stage, Draco.Stage.WARNING, "boss warning paused");
        q.command(GameCommand.Action.PAUSE); q.advance(1000); equal(q.game.boss.stage, Draco.Stage.ACTIVE, "boss warning resumed");
    }
    private static void restart() throws Exception {
        Rig r = playing(); GameSnapshot original = r.game.snapshot();
        r.move(Direction.RIGHT); r.command(new GameCommand.Select(BombType.REMOTE)); r.command(GameCommand.Action.PLACE);
        r.game.score = 300; r.game.player.collect(PowerUpType.BOMB_UP); r.advance(1000);
        r.command(GameCommand.Action.PAUSE); r.command(GameCommand.Action.RESTART); GameSnapshot reset = r.game.snapshot();
        equal(reset.board(), original.board(), "layout restored"); equal(reset.player(), original.player(), "player restored");
        equal(reset.enemies(), original.enemies(), "enemies restored"); equal(reset.score(), 0, "score reset"); equal(reset.elapsedMillis(), 0L, "time reset");
        check(reset.bombs().isEmpty() && reset.explosions().isEmpty() && reset.warning().isEmpty(), "transients reset");
    }
    private static void concurrency() throws Exception {
        Rig r = playing(); AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread t = new Thread(() -> { try { r.game.accept(GameCommand.Action.PLACE); } catch (Throwable e) { failure.set(e); } });
        t.start(); t.join(); check(failure.get() instanceof IllegalStateException, "wrong thread rejected");
        try { r.game.snapshot().board().add("bad"); throw new AssertionError("mutable snapshot"); }
        catch (UnsupportedOperationException expected) { }
        CommandQueue<Integer> queue = new CommandQueue<>(8);
        Thread producer = new Thread(() -> { try { for (int i = 0; i < 2000; i++) queue.put(i); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        producer.start();
        for (int i = 0; i < 2000;) { Integer value = queue.poll(); if (value != null) equal(value, i++, "FIFO delivery"); else Thread.yield(); }
        producer.join(1000); check(!producer.isAlive(), "bounded queue no deadlock");
    }
    private static void input() throws Exception {
        CommandQueue<Integer> queue = new CommandQueue<>(32);
        new KeyboardReader(new java.io.ByteArrayInputStream("w\033[A\033[B\033[C\033[Dbq".getBytes(java.nio.charset.StandardCharsets.UTF_8)), queue).run();
        for (int expected : new int[]{'w', KeyBindings.UP, KeyBindings.DOWN, KeyBindings.RIGHT, KeyBindings.LEFT, 'b', 'q', -1})
            equal(queue.poll(), expected, "key decoder");
        equal(KeyBindings.decode('q', GameState.PLAYING), GameCommand.Action.DETONATE, "Q playing");
        equal(KeyBindings.decode('q', GameState.PAUSED), GameCommand.Action.EXIT, "Q paused");
        equal(KeyBindings.decode('x', GameState.MENU), GameCommand.Action.INVALID, "bad menu key");
        for (Direction direction : Direction.values())
            equal(KeyBindings.decode(KeyBindings.UP + direction.ordinal(), GameState.PLAYING),
                    new GameCommand.Move(direction), "arrow binding " + direction);
    }
}
