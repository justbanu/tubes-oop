package nangor.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static nangor.core.TestSupport.*;

/** Plays the shipped map through public commands. No test writes to the world. */
public final class CampaignTest {
    private final Rig rig;
    private final List<String> trace = new ArrayList<>();
    private CampaignTest(long seed) throws Exception { rig = new Rig(seed); }
    private GameSnapshot view() { return rig.game.snapshot(); }
    private void send(GameCommand c) { trace.add(view().elapsedMillis() + " " + c); rig.command(c); }
    private void waitFor(long ms) {
        for (long n = 0; n < ms; n += 10) {
            Set<Position> area = new HashSet<>();
            for (var bomb : view().bombs()) if (bomb.armed() && bomb.type() == BombType.REMOTE) area.addAll(blast(bomb.position(), bomb.range()));
            if (!area.contains(view().player().position()) && view().enemies().stream()
                    .anyMatch(e -> area.contains(e.position()) && (e.kind().equals("NORMAL") || view().vulnerable())))
                send(GameCommand.Action.DETONATE);
            rig.advance(Math.min(10, ms - n));
        }
        check(view().state() == GameState.PLAYING || view().state() == GameState.WIN,
                "Campaign ended: " + view().state() + " at " + view().elapsedMillis() + " score=" + view().score());
    }
    private boolean open(Position p) {
        if (!MapDefinition.inside(p)) return false;
        return "#DRoO@".indexOf(view().board().get(p.y()).charAt(p.x())) < 0
                && view().enemies().stream().noneMatch(e -> e.position().equals(p));
    }
    private Set<Position> danger() {
        Set<Position> result = new HashSet<>(view().explosions()); result.addAll(view().warning()); result.addAll(view().attack()); return result;
    }
    private List<Position> path(Position to, Set<Position> forbidden) {
        Position start = view().player().position();
        ArrayDeque<Position> queue = new ArrayDeque<>(); Map<Position, Position> prev = new HashMap<>();
        prev.put(start, start); queue.add(start);
        while (!queue.isEmpty()) {
            Position p = queue.remove();
            if (p.equals(to)) {
                List<Position> route = new ArrayList<>();
                while (!p.equals(start)) { route.add(0, p); p = prev.get(p); }
                return route;
            }
            for (Direction d : Direction.values()) {
                Position next = p.step(d);
                if (prev.containsKey(next) || !open(next) || forbidden.contains(next)) continue;
                prev.put(next, p); queue.add(next);
            }
        }
        return List.of();
    }
    private void step(Position next) {
        Position current = view().player().position();
        for (Direction d : Direction.values()) if (current.step(d).equals(next)) { send(new GameCommand.Move(d)); waitFor(300); return; }
        throw new AssertionError("Nonadjacent route step");
    }
    private void go(int x, int y) { go(new Position(x, y)); }
    private void go(Position target) {
        for (int attempts = 0; attempts < 150; attempts++) {
            if (view().player().position().equals(target)) return;
            List<Position> route = path(target, danger());
            if (route.isEmpty()) waitFor(100); else step(route.get(0));
        }
        throw new AssertionError("No route to " + target + " at " + view().player().position());
    }
    private Set<Position> blast(Position center, int range) {
        Set<Position> area = new HashSet<>(); area.add(center);
        for (Direction d : Direction.values()) {
            Position p = center;
            for (int i = 0; i < range; i++) {
                p = p.step(d);
                if (!MapDefinition.inside(p) || "#DR".indexOf(view().board().get(p.y()).charAt(p.x())) >= 0) break;
                area.add(p);
            }
        }
        return area;
    }
    private void clearBlock(int bx, int by, int sx, int sy) {
        go(bx, by); send(new GameCommand.Select(BombType.REMOTE)); send(GameCommand.Action.PLACE);
        go(sx, sy); send(GameCommand.Action.DETONATE); waitFor(600);
    }
    private void trap(int bx, int by, int sx, int sy, long enemyId) {
        if (view().enemies().stream().noneMatch(e -> e.id() == enemyId)) return;
        go(bx, by); send(new GameCommand.Select(BombType.REMOTE)); send(GameCommand.Action.PLACE); go(sx, sy);
        for (int i = 0; i < 200; i++) {
            if (view().enemies().stream().noneMatch(e -> e.id() == enemyId)) { waitFor(600); return; }
            Set<Position> area = new HashSet<>();
            for (var bomb : view().bombs()) if (bomb.armed()) area.addAll(blast(bomb.position(), bomb.range()));
            if (!area.contains(view().player().position()) && view().enemies().stream().anyMatch(e -> e.id() == enemyId && area.contains(e.position()))) {
                send(GameCommand.Action.DETONATE); waitFor(600); return;
            }
            waitFor(100);
        }
        throw new AssertionError("Enemy never enters trap " + enemyId + " at " + bx + "," + by + " pos=" + view().enemies());
    }
    private void pumpedTrap(int bx, int by, int sx, int sy, long enemyId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            if (view().enemies().stream().noneMatch(e -> e.id() == enemyId)) return;
            go(bx, by); send(new GameCommand.Select(BombType.PUMPED)); send(GameCommand.Action.PLACE);
            go(sx, sy); waitFor(5100);
        }
        throw new AssertionError("Pumped trap did not hit " + enemyId);
    }
    private void campaign() {
        send(GameCommand.Action.START); go(2, 1);
        clearBlock(3, 1, 1, 1);
        trap(3, 2, 2, 1, 4); // Chase enemy in western passage.
        pumpedTrap(7, 1, 3, 2, 2);
        trap(12, 3, 11, 2, 3);
        pumpedTrap(12, 5, 11, 2, 6);
        trap(6, 6, 7, 5, 5);
        System.out.println("Campaign normals cleared: " + view().elapsedMillis() + "ms lives=" + view().player().lives());
        fightBoss();
        go(12, 1);
        equal(view().state(), GameState.WIN, "full campaign WIN");
    }
    private void fightBoss() {
        for (int attempt = 0; attempt < 2000; attempt++) {
            var bosses = view().enemies().stream().filter(e -> e.kind().equals("DRACO")).toList();
            if (bosses.isEmpty()) return;
            Position boss = bosses.get(0).position();
            Set<Position> danger = danger(); Position player = view().player().position();
            if (danger.contains(player)) {
                Position safe = nearestSafe(danger, Set.of());
                if (safe != null) { List<Position> route = path(safe, Set.of()); if (!route.isEmpty()) step(route.get(0)); }
                else waitFor(100);
                continue;
            }
            if (view().bombs().stream().anyMatch(GameSnapshot.BombView::armed)) {
                Set<Position> area = new HashSet<>();
                for (var b : view().bombs()) if (b.armed()) area.addAll(blast(b.position(), b.range()));
                if (area.contains(player)) {
                    Position safe = nearestSafe(area, danger);
                    if (safe != null) {
                        List<Position> route = path(safe, danger);
                        if (!route.isEmpty()) step(route.get(0)); else waitFor(100);
                    } else waitFor(100);
                } else if (view().vulnerable() && area.contains(boss)) {
                    send(GameCommand.Action.DETONATE); waitFor(600);
                } else if (!area.contains(boss)) { send(GameCommand.Action.DETONATE); waitFor(600); }
                else bait(boss, area);
                continue;
            }
            if (view().vulnerable()) {
                if (player.distance(boss) == 1) { send(new GameCommand.Select(BombType.REMOTE)); send(GameCommand.Action.PLACE); continue; }
                Position chosen = null; int shortest = 1000;
                for (Direction d : Direction.values()) {
                    Position p = boss.step(d); if (!open(p) || danger.contains(p)) continue;
                    List<Position> route = path(p, danger);
                    if (!route.isEmpty() && route.size() < shortest) { chosen = route.get(0); shortest = route.size(); }
                }
                if (chosen != null) step(chosen); else waitFor(100);
            } else {
                bait(boss, Set.of());
            }
        }
        throw new AssertionError("Boss fight timed out " + view());
    }
    private void bait(Position boss, Set<Position> bombs) {
        Set<Position> forbidden = danger(); forbidden.addAll(bombs);
        Position chosen = null; int shortest = 1000;
        for (Direction d : Direction.values()) {
            Position p = boss.step(d).step(d);
            if (!open(p) || forbidden.contains(p)) continue;
            if (p.equals(view().player().position())) { chosen = p; break; }
            List<Position> route = path(p, forbidden);
            if (!route.isEmpty() && route.size() < shortest) { chosen = route.get(0); shortest = route.size(); }
        }
        if (chosen != null && !chosen.equals(view().player().position())) step(chosen); else waitFor(100);
    }
    private Position nearestSafe(Set<Position> hazard, Set<Position> forbidden) {
        Position best = null; int distance = 1000;
        for (int y = 1; y < 9; y++) for (int x = 1; x < 13; x++) {
            Position p = new Position(x, y); if (hazard.contains(p) || forbidden.contains(p) || !open(p)) continue;
            List<Position> route = path(p, forbidden);
            if (!route.isEmpty() && route.size() < distance) { best = p; distance = route.size(); }
        }
        return best;
    }
    public static void main(String[] args) throws Exception {
        CampaignTest test = new CampaignTest(args.length == 0 ? 17 : Long.parseLong(args[0]));
        try {
            test.campaign();
            System.out.println("Campaign WIN: time=" + test.view().elapsedMillis() + " lives=" + test.view().player().lives() + " score=" + test.view().score());
        } finally {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("build"));
            java.nio.file.Files.write(java.nio.file.Path.of("build/campaign-trace.txt"), test.trace);
            System.out.println(nangor.terminal.TerminalRenderer.text(test.view()));
        }
    }
}
