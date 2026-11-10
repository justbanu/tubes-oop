package nangor.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nangor.terminal.TerminalRenderer;
import static nangor.core.TestSupport.*;

public final class TerminalRendererTest {
    private TerminalRendererTest() { }

    public static void main(String[] args) throws Exception {
        Rig rig = new Rig();
        Map<GameState, String> screens = new LinkedHashMap<>();
        screens.put(GameState.MENU, TerminalRenderer.text(rig.game.snapshot()));
        rig.command(GameCommand.Action.HELP);
        screens.put(GameState.HELP, TerminalRenderer.text(rig.game.snapshot()));
        rig.command(GameCommand.Action.BACK);
        rig.start();
        GameSnapshot initial = rig.game.snapshot();
        screens.put(GameState.PLAYING, TerminalRenderer.text(initial));
        rig.command(GameCommand.Action.PAUSE);
        screens.put(GameState.PAUSED, TerminalRenderer.text(rig.game.snapshot()));
        rig.command(GameCommand.Action.PAUSE);
        rig.advance(300_000);
        GameSnapshot loss = rig.game.snapshot();
        screens.put(GameState.LOSE, TerminalRenderer.text(loss));
        List<String> wonBoard = new ArrayList<>();
        for (int y = 0; y < loss.board().size(); y++) {
            char[] row = loss.board().get(y).replace('E', '.').replace('B', '.').toCharArray();
            if (y == 1) {
                row[1] = '.';
                row[12] = 'P';
            }
            wonBoard.add(new String(row));
        }
        GameSnapshot.PlayerView player = loss.player();
        GameSnapshot.PlayerView winner = new GameSnapshot.PlayerView(new Position(12, 1), 3,
                player.capacity(), 0, player.range(), player.speed(), player.selected(),
                player.remote(), false);
        GameSnapshot win = new GameSnapshot(GameState.WIN, wonBoard, winner, List.of(),
                List.of(), Set.of(), Set.of(), Set.of(), 3260, 103_600, 196,
                "Green Garden berhasil diselamatkan!", "DEFEATED", null, 2, false);
        screens.put(GameState.WIN, TerminalRenderer.text(win));

        for (var screen : screens.entrySet()) {
            for (String line : screen.getValue().split("\n"))
                check(line.length() <= 80, screen.getKey() + " exceeds 80 columns: " + line);
        }
        check(screens.get(GameState.MENU).contains("1  Start Game"), "menu actions visible");
        check(screens.get(GameState.HELP).contains("LEGENDA MAP"), "full legend moved to help");
        check(screens.get(GameState.HELP).contains("+ Bomb Up  f Fire Up"), "power-up legend in help");
        for (GameState state : new GameState[]{GameState.PLAYING, GameState.PAUSED, GameState.WIN, GameState.LOSE}) {
            String screen = screens.get(state);
            check(screen.contains("Nyawa ") && screen.contains("Waktu ") && screen.contains("Bomb "), state + " HUD");
            check(screen.contains("Pesan: "), state + " last message");
            check(!screen.contains("LEGENDA MAP") && !screen.contains("P Deeper"), state + " no repeated legend");
            String[] lines = screen.split("\n");
            String header = java.util.Arrays.stream(lines).filter(line -> line.startsWith("    01 02")).findFirst().orElseThrow();
            int index = java.util.Arrays.asList(lines).indexOf(header);
            check(header.length() == 46, state + " column header width");
            GameSnapshot expected = switch (state) {
                case PLAYING, PAUSED -> initial;
                case WIN -> win;
                default -> loss;
            };
            for (int y = 0; y < 10; y++) {
                String row = lines[index + y + 1];
                check(row.length() == 46 && row.startsWith(String.format("%02d  ", y + 1)), state + " row width");
                for (int x = 0; x < 14; x++)
                    equal(row.charAt(4 + x * 3), expected.board().get(y).charAt(x), state + " tile alignment");
            }
            check(screen.lines().filter(line -> line.startsWith("Pesan: ")).count() == 1, state + " one message");
        }
        check(screens.get(GameState.PLAYING).contains("WASD/panah gerak"), "playing controls");
        check(screens.get(GameState.PAUSED).contains("PAUSED: P lanjut  |  R restart  |  M menu  |  Q keluar"), "pause choices");
        check(screens.get(GameState.WIN).contains("SKOR AKHIR 3260"), "WIN final score");
        check(screens.get(GameState.LOSE).contains("SKOR AKHIR 0"), "LOSE final score");

        System.out.println("TerminalRendererTest: PASS MENU, PLAYING, PAUSED, WIN, LOSE; 80-column width and grid alignment");
        if (args.length > 0 && args[0].equals("--preview"))
            screens.forEach((state, content) -> System.out.println("=== " + state + " ===\n" + content));
    }
}
