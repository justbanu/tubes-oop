package nangor.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Immutable, validated configuration; every restart builds fresh entities from it. */
public final class MapDefinition {
    public static final int WIDTH = 14, HEIGHT = 10;
    public record EnemySpec(boolean chase, List<Position> route) {
        public EnemySpec { route = List.copyOf(route); }
    }
    public record Arena(int left, int top, int right, int bottom) {
        public boolean contains(Position p) {
            return p.x() >= left && p.x() <= right && p.y() >= top && p.y() <= bottom;
        }
    }

    final List<String> rows;
    final Position spawn, bossSpawn, exit, remote;
    final List<EnemySpec> enemies;
    final Arena arena;

    private MapDefinition(List<String> rows, Position spawn, Position bossSpawn,
                          Position exit, Position remote, List<EnemySpec> enemies, Arena arena) {
        this.rows = List.copyOf(rows); this.spawn = spawn; this.bossSpawn = bossSpawn;
        this.exit = exit; this.remote = remote; this.enemies = List.copyOf(enemies); this.arena = arena;
    }

    public static MapDefinition load(Path directory) throws MapValidationException {
        try (InputStream map = directory == null
                ? MapDefinition.class.getResourceAsStream("/config/green-garden.map")
                : Files.newInputStream(directory.resolve("green-garden.map"));
             InputStream settings = directory == null
                ? MapDefinition.class.getResourceAsStream("/config/green-garden.properties")
                : Files.newInputStream(directory.resolve("green-garden.properties"))) {
            if (map == null || settings == null) throw new IOException("Konfigurasi tidak ditemukan");
            Properties properties = new Properties(); properties.load(settings);
            return parse(new String(map.readAllBytes(), StandardCharsets.UTF_8).lines().toList(), properties);
        } catch (IOException e) {
            throw new MapValidationException("Gagal membaca map: " + e.getMessage(), e);
        }
    }

    static MapDefinition parse(List<String> rows, Properties settings) throws MapValidationException {
        if (rows.size() != HEIGHT || rows.stream().anyMatch(r -> r.length() != WIDTH))
            throw new MapValidationException("Map harus berukuran 14 x 10 tile");
        List<Position> players = new ArrayList<>(), bosses = new ArrayList<>(), exits = new ArrayList<>(), spawns = new ArrayList<>();
        int destructible = 0, reinforced = 0;
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            char c = rows.get(y).charAt(x); Position p = new Position(x, y);
            if ("#.DRPEBX".indexOf(c) < 0) throw new MapValidationException("Simbol map A tidak dikenal: " + c);
            if ((x == 0 || y == 0 || x == WIDTH - 1 || y == HEIGHT - 1) && c != '#')
                throw new MapValidationException("Batas map harus berupa #");
            switch (c) {
                case 'P' -> players.add(p); case 'B' -> bosses.add(p); case 'X' -> exits.add(p);
                case 'E' -> spawns.add(p); case 'D' -> destructible++; case 'R' -> reinforced++;
                default -> { }
            }
        }
        if (players.size() != 1 || bosses.size() != 1 || exits.size() != 1 || spawns.size() != 5)
            throw new MapValidationException("Diperlukan tepat 1 P, 5 E, 1 B, dan 1 X");
        if (destructible == 0 || reinforced == 0) throw new MapValidationException("Map A memerlukan block D dan R");
        try {
            Position remote = position(settings.getProperty("remote"));
            int[] a = java.util.Arrays.stream(settings.getProperty("arena", "").split(",")).mapToInt(Integer::parseInt).toArray();
            if (a.length != 4) throw new IllegalArgumentException("arena perlu 4 koordinat");
            Arena arena = new Arena(a[0], a[1], a[2], a[3]);
            if (a[0] < 1 || a[1] < 1 || a[2] >= WIDTH - 1 || a[3] >= HEIGHT - 1 || a[0] > a[2] || a[1] > a[3]
                    || !arena.contains(bosses.get(0))) throw new IllegalArgumentException("Area Draco tidak valid");
            if (!inside(remote) || rows.get(remote.y()).charAt(remote.x()) != '.')
                throw new IllegalArgumentException("Remote Detonator harus di tile kosong");
            List<EnemySpec> enemies = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String[] fields = settings.getProperty("enemy." + i, "").split(";");
                if (fields.length < 3 || !(fields[0].equals("PATROL") || fields[0].equals("CHASE")))
                    throw new IllegalArgumentException("Rute enemy " + i + " tidak valid");
                List<Position> route = new ArrayList<>();
                for (int j = 1; j < fields.length; j++) {
                    Position p = position(fields[j]);
                    if (!inside(p) || "#DR".indexOf(rows.get(p.y()).charAt(p.x())) >= 0)
                        throw new IllegalArgumentException("Rute patrol terhalang");
                    if (!route.isEmpty() && route.get(route.size() - 1).distance(p) != 1)
                        throw new IllegalArgumentException("Rute patrol harus bertetangga");
                    route.add(p);
                }
                if (!route.get(0).equals(spawns.get(i))) throw new IllegalArgumentException("Rute tidak dimulai pada spawn enemy " + i);
                enemies.add(new EnemySpec(fields[0].equals("CHASE"), route));
            }
            Set<Position> reachable = flood(rows, players.get(0), true);
            List<Position> targets = new ArrayList<>(spawns); targets.add(bosses.get(0)); targets.add(exits.get(0)); targets.add(remote);
            if (!reachable.containsAll(targets)) throw new IllegalArgumentException("Spawn/boss/exit/Remote tidak dapat dicapai setelah D/R dihancurkan");
            if (!flood(rows, players.get(0), false).contains(remote))
                throw new IllegalArgumentException("Remote jaminan harus dapat dicapai tanpa menghancurkan block");
            return new MapDefinition(rows, players.get(0), bosses.get(0), exits.get(0), remote, enemies, arena);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new MapValidationException("Konfigurasi map tidak valid: " + e.getMessage(), e);
        }
    }

    private static Position position(String value) {
        if (value == null) throw new IllegalArgumentException("Koordinat hilang");
        String[] xy = value.split(",");
        if (xy.length != 2) throw new IllegalArgumentException("Koordinat perlu x,y");
        return new Position(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
    }

    private static Set<Position> flood(List<String> rows, Position start, boolean canBreak) {
        Set<Position> seen = new HashSet<>(); ArrayDeque<Position> queue = new ArrayDeque<>();
        seen.add(start); queue.add(start);
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            for (Direction d : Direction.values()) {
                Position next = current.step(d);
                if (inside(next) && rows.get(next.y()).charAt(next.x()) != '#'
                        && (canBreak || "DR".indexOf(rows.get(next.y()).charAt(next.x())) < 0) && seen.add(next)) queue.add(next);
            }
        }
        return seen;
    }

    public static boolean inside(Position p) { return p.x() >= 0 && p.y() >= 0 && p.x() < WIDTH && p.y() < HEIGHT; }
    public List<String> rows() { return rows; }
}
