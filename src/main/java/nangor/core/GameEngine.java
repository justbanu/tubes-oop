package nangor.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** Single writer of the world. Input and rendering use commands and immutable snapshots. */
public final class GameEngine {
    static final long LIMIT = 300_000, STEP_NANOS = 10_000_000;
    final MapDefinition definition;
    final RandomGenerator random;
    private final NanoClock clock;
    private final Thread owner = Thread.currentThread();
    private long lastNanos, pendingNanos, nextId;
    long now;
    GameState state = GameState.MENU;
    Player player;
    Draco boss;
    final EntityStore<Enemy> enemies = new EntityStore<>();
    final EntityStore<Bomb> bombs = new EntityStore<>();
    final EntityStore<Explosion> explosions = new EntityStore<>();
    final Map<Position, Block> blocks = new LinkedHashMap<>();
    final Map<Position, PowerUpType> powerUps = new LinkedHashMap<>();
    int score;
    String message = "Selamat datang di Green Garden.";

    public GameEngine(MapDefinition definition, RandomGenerator random, NanoClock clock) {
        this.definition = java.util.Objects.requireNonNull(definition);
        this.random = java.util.Objects.requireNonNull(random);
        this.clock = java.util.Objects.requireNonNull(clock);
        lastNanos = clock.nanoTime();
    }

    private void assertOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("State game hanya boleh diakses thread pemilik");
    }

    public void accept(GameCommand command) {
        assertOwner(); java.util.Objects.requireNonNull(command).execute(this);
        if (state == GameState.PLAYING) { applyDamage(); checkEnd(); }
    }

    public void pulse() {
        assertOwner();
        long current = clock.nanoTime(), delta = current - lastNanos;
        if (delta < 0) throw new IllegalStateException("Clock harus monotonic");
        lastNanos = current;
        if (state != GameState.PLAYING) return;
        pendingNanos += delta;
        while (pendingNanos >= STEP_NANOS && state == GameState.PLAYING) {
            pendingNanos -= STEP_NANOS; now += 10;
            tick();
        }
    }

    void action(GameCommand.Action action) {
        assertOwner();
        switch (action) {
            case EXIT -> state = GameState.EXIT;
            case START -> { if (state == GameState.MENU) reset(); else invalid(); }
            case HELP -> { if (state == GameState.MENU) state = GameState.HELP; else invalid(); }
            case BACK -> {
                if (state != GameState.PLAYING) { state = GameState.MENU; message = "Pilih 1, 2, atau 3."; }
                else invalid();
            }
            case PAUSE -> {
                if (state == GameState.PLAYING) { state = GameState.PAUSED; message = "Permainan dijeda."; }
                else if (state == GameState.PAUSED) { state = GameState.PLAYING; lastNanos = clock.nanoTime(); message = "Permainan dilanjutkan."; }
                else invalid();
            }
            case RESTART -> {
                if (state == GameState.PAUSED || state == GameState.WIN || state == GameState.LOSE) reset();
                else invalid();
            }
            case PLACE -> { if (state == GameState.PLAYING) place(); }
            case DETONATE -> {
                if (state == GameState.PLAYING) {
                    List<Bomb> remote = bombs.all().stream().filter(b -> b.armed && b.type == BombType.REMOTE && b.owner == player.id).toList();
                    if (remote.isEmpty()) message = "Tidak ada Remote Bomb aktif.";
                    else detonate(remote);
                }
            }
            case INVALID -> invalid();
        }
    }

    private void invalid() { message = "Input tidak valid untuk menu ini."; }

    private void reset() {
        now = 0; pendingNanos = 0; lastNanos = clock.nanoTime(); nextId = 1; score = 0;
        bombs.clear(); explosions.clear(); enemies.clear(); blocks.clear(); powerUps.clear();
        player = new Player(nextId++, definition.spawn);
        for (int y = 0; y < MapDefinition.HEIGHT; y++) for (int x = 0; x < MapDefinition.WIDTH; x++) {
            Block block = switch (definition.rows.get(y).charAt(x)) {
                case '#' -> Block.INDESTRUCTIBLE; case 'D' -> Block.DESTRUCTIBLE; case 'R' -> Block.REINFORCED; default -> null;
            };
            if (block != null) blocks.put(new Position(x, y), block);
        }
        for (MapDefinition.EnemySpec spec : definition.enemies) enemies.add(new NormalEnemy(nextId++, spec));
        boss = new Draco(nextId++, definition.bossSpawn); enemies.add(boss);
        powerUps.put(definition.remote, PowerUpType.REMOTE);
        state = GameState.PLAYING; message = "Kalahkan 5 enemy dan Draco, lalu masuk X.";
    }

    void move(Direction direction) {
        assertOwner();
        if (state != GameState.PLAYING || now < player.nextMove) return;
        player.direction = direction;
        Position next = player.position.step(direction);
        if (!walkable(next) || bombAt(next) != null) return;
        if (enemyAt(next) != null) { hurtPlayer(); return; }
        player.nextMove = now + player.moveInterval(); player.position = next;
        PowerUpType power = powerUps.remove(next);
        if (power != null) { player.collect(power); message = "Power-up: " + power; }
    }

    void select(BombType type) {
        assertOwner();
        if (state != GameState.PLAYING) return;
        if (type == BombType.REMOTE && !player.remoteUnlocked) { message = "Ambil r untuk membuka Remote Bomb."; return; }
        player.selected = type; message = "Bomb terpilih: " + type;
    }

    private void place() {
        if (bombs.size() >= player.bombCapacity) { message = "Kapasitas bomb penuh."; return; }
        if (!walkable(player.position) || bombAt(player.position) != null || enemyAt(player.position) != null) {
            message = "Tile tidak dapat ditempati bomb."; return;
        }
        Bomb bomb = player.selected.create(nextId++, player, now); bombs.add(bomb);
        message = "Memasang " + player.selected + ". Segera menjauh!";
        // A bomb placed in an existing flame chains immediately, including Remote Bombs.
        if (explosions.all().stream().anyMatch(e -> e.tiles.contains(bomb.position))) detonate(List.of(bomb));
    }

    private void tick() {
        explosions.removeIf(e -> now >= e.endAt);
        bombs.removeIf(b -> !b.armed && now >= b.releaseAt);
        detonate(bombs.all().stream().filter(b -> b.armed && b.due(now)).toList());
        applyDamage();
        if (player.lives <= 0) { checkEnd(); return; }
        for (Enemy enemy : enemies.all()) {
            enemy.update(this);
            if (player.lives <= 0) break;
        }
        applyDamage(); checkEnd();
    }

    void detonate(List<Bomb> triggers) {
        ArrayDeque<Bomb> queue = new ArrayDeque<>(triggers);
        Set<Long> queued = new HashSet<>(); for (Bomb b : triggers) queued.add(b.id);
        while (!queue.isEmpty()) {
            Bomb bomb = queue.remove();
            if (!bomb.armed) continue;
            bomb.armed = false; bomb.releaseAt = now + 500;
            Set<Position> area = new LinkedHashSet<>(); area.add(bomb.position);
            for (Direction direction : Direction.values()) {
                Position p = bomb.position;
                for (int i = 0; i < bomb.range; i++) {
                    p = p.step(direction);
                    if (!MapDefinition.inside(p)) break;
                    Block block = blocks.get(p);
                    if (block != null) {
                        if (block.destroyedBy(bomb.type)) {
                            blocks.remove(p);
                            if (block == Block.DESTRUCTIBLE && random.nextDouble() < 0.30) {
                                PowerUpType[] types = PowerUpType.values(); powerUps.put(p, types[random.nextInt(types.length)]);
                            }
                        }
                        break; // A destroyed block's tile is excluded from this activation.
                    }
                    area.add(p);
                }
            }
            Explosion explosion = new Explosion(nextId++, bomb, area, now); explosions.add(explosion);
            for (Bomb other : bombs.all()) if (other.armed && area.contains(other.position) && queued.add(other.id)) queue.add(other);
        }
    }

    private void applyDamage() {
        for (Explosion explosion : explosions.all()) {
            if (now >= explosion.endAt) continue;
            if (explosion.tiles.contains(player.position) && !explosion.damaged.contains(player.id) && hurtPlayer())
                explosion.damaged.add(player.id);
            for (Enemy enemy : enemies.all()) {
                if (!enemy.alive() || !explosion.tiles.contains(enemy.position) || explosion.damaged.contains(enemy.id)) continue;
                if (enemy instanceof Draco draco && !draco.vulnerable(now)) continue;
                explosion.damaged.add(enemy.id); enemy.health = Math.max(0, enemy.health - explosion.type.damage);
                if (!enemy.alive()) { score += enemy.scoreValue; message = enemy instanceof Draco ? "Draco dikalahkan!" : "Enemy dikalahkan! +100"; }
            }
        }
        if (boss.alive() && boss.stage == Draco.Stage.ACTIVE && boss.pattern != AttackPattern.SWOOP && boss.area.contains(player.position)
                && !boss.hitPlayer && hurtPlayer()) boss.hitPlayer = true;
    }

    boolean hurtPlayer() {
        if (player.lives <= 0 || player.invulnerable(now)) return false;
        player.lives--; message = "Deeper terkena serangan. Nyawa: " + player.lives;
        if (player.lives > 0) {
            player.invulnerableUntil = now + 1500;
            player.position = respawn(); player.nextMove = now;
        }
        return true;
    }

    private Position respawn() {
        ArrayDeque<Position> queue = new ArrayDeque<>(); Set<Position> visited = new HashSet<>();
        queue.add(definition.spawn); visited.add(definition.spawn);
        while (!queue.isEmpty()) {
            Position p = queue.remove();
            if (walkable(p) && bombAt(p) == null && enemyAt(p) == null) return p;
            for (Direction d : Direction.values()) {
                Position next = p.step(d);
                if (walkable(next) && visited.add(next)) queue.add(next);
            }
        }
        throw new IllegalStateException("Tidak tersedia tile respawn");
    }

    private void checkEnd() {
        if (state != GameState.PLAYING) return;
        if (player.lives <= 0 || now >= LIMIT) { state = GameState.LOSE; message = player.lives <= 0 ? "Nyawa habis." : "Waktu habis."; }
        else if (player.position.equals(definition.exit) && enemies.all().stream().noneMatch(Enemy::alive)) {
            score += player.lives * 100 + secondsLeft() * 10;
            state = GameState.WIN; message = "Green Garden berhasil diselamatkan!";
        }
    }

    int secondsLeft() { return (int) Math.max(0, (LIMIT - now) / 1000); }
    boolean walkable(Position p) { return MapDefinition.inside(p) && !blocks.containsKey(p); }
    Bomb bombAt(Position p) { return bombs.all().stream().filter(b -> b.armed && b.position.equals(p)).findFirst().orElse(null); }
    Enemy enemyAt(Position p) { return enemies.all().stream().filter(e -> e.alive() && e.position.equals(p)).findFirst().orElse(null); }
    boolean enemyCanEnter(Position p, Enemy self) {
        Enemy other = enemyAt(p);
        return walkable(p) && bombAt(p) == null && (other == null || other == self);
    }
    boolean bossCanEnter(Position p) {
        Enemy other = enemyAt(p);
        return definition.arena.contains(p) && walkable(p) && bombAt(p) == null && (other == null || other == boss);
    }

    Position pathStep(Position start, Position target, Predicate<Position> passable) {
        if (start.equals(target)) return start;
        ArrayDeque<Position> queue = new ArrayDeque<>(); Map<Position, Position> previous = new HashMap<>();
        queue.add(start); previous.put(start, start);
        while (!queue.isEmpty()) {
            Position p = queue.remove();
            for (Direction d : Direction.values()) {
                Position next = p.step(d);
                if (previous.containsKey(next) || !passable.test(next)) continue;
                previous.put(next, p);
                if (next.equals(target)) {
                    while (!previous.get(next).equals(start)) next = previous.get(next);
                    return next;
                }
                queue.add(next);
            }
        }
        return start;
    }

    public GameState state() { assertOwner(); return state; }

    public GameSnapshot snapshot() {
        assertOwner();
        if (player == null) return new GameSnapshot(state, List.of(), null, List.of(), List.of(), Set.of(), Set.of(), Set.of(),
                0, 0, 300, message, "WAITING", null, 1, false);
        char[][] grid = new char[MapDefinition.HEIGHT][MapDefinition.WIDTH];
        for (char[] row : grid) java.util.Arrays.fill(row, '.');
        blocks.forEach((p, b) -> grid[p.y()][p.x()] = b.symbol);
        grid[definition.exit.y()][definition.exit.x()] = 'X';
        powerUps.forEach((p, power) -> grid[p.y()][p.x()] = power.symbol);
        Set<Position> flames = new HashSet<>(); for (Explosion e : explosions.all()) flames.addAll(e.tiles);
        Set<Position> warning = boss.alive() && boss.stage == Draco.Stage.WARNING ? boss.area : Set.of();
        Set<Position> attack = boss.alive() && boss.stage == Draco.Stage.ACTIVE ? boss.area : Set.of();
        warning.forEach(p -> grid[p.y()][p.x()] = '!'); attack.forEach(p -> grid[p.y()][p.x()] = '^');
        flames.forEach(p -> grid[p.y()][p.x()] = '*');
        for (Bomb b : bombs.all()) if (b.armed) grid[b.position.y()][b.position.x()] = b.type.symbol;
        List<GameSnapshot.EnemyView> enemyViews = new ArrayList<>();
        for (Enemy enemy : enemies.all()) if (enemy.alive()) {
            grid[enemy.position.y()][enemy.position.x()] = enemy instanceof Draco ? 'B' : 'E';
            enemyViews.add(new GameSnapshot.EnemyView(enemy.id, enemy.position, enemy.health,
                    enemy instanceof Draco ? "DRACO" : "NORMAL", enemy instanceof NormalEnemy n && n.chasing ? "CHASE" : "PATROL"));
        }
        grid[player.position.y()][player.position.x()] = player.invulnerable(now) ? 'p' : 'P';
        List<String> board = new ArrayList<>(); for (char[] row : grid) board.add(new String(row));
        return new GameSnapshot(state, board, new GameSnapshot.PlayerView(player.position, player.lives, player.bombCapacity, bombs.size(),
                player.blastRange, 1 + 0.5 * player.speedLevel, player.selected, player.remoteUnlocked, player.invulnerable(now)),
                enemyViews, bombs.all().stream().map(b -> new GameSnapshot.BombView(b.position, b.type, b.range, b.armed)).toList(),
                flames, warning, attack, score, now, secondsLeft(), message, boss.alive() ? boss.stage.name() : "DEFEATED", boss.pattern, boss.phase(), boss.vulnerable(now));
    }
}
