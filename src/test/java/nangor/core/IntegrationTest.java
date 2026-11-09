package nangor.core;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import nangor.runtime.CommandQueue;
import nangor.runtime.GameLoop;
import nangor.runtime.KeyboardReader;
import nangor.terminal.TerminalRenderer;
import static nangor.core.TestSupport.*;

public final class IntegrationTest {
    private IntegrationTest() { }
    public static void main(String[] args) throws Exception {
        Rig rig = new Rig(); CommandQueue<Integer> input = new CommandQueue<>(32);
        List<GameSnapshot> rendered = new ArrayList<>(); GameLoop loop = new GameLoop(rig.game, input, rendered::add);
        loop.frame(); equal(rig.game.state(), GameState.MENU, "initial menu");
        key(input, loop, 'x'); check(rig.game.snapshot().message().contains("tidak valid"), "menu rejects bad input");
        key(input, loop, '2'); equal(rig.game.state(), GameState.HELP, "help");
        check(TerminalRenderer.text(rig.game.snapshot()).contains("Draco rentan"), "help explains vulnerability");
        key(input, loop, 'm'); key(input, loop, '1'); equal(rig.game.state(), GameState.PLAYING, "start via queue");
        key(input, loop, 'd'); key(input, loop, '3'); key(input, loop, 'b');
        check(rig.game.snapshot().player().remote(), "remote picked up during movement");
        equal(rig.game.snapshot().bombs().size(), 1, "remote planted");
        key(input, loop, 'p'); GameSnapshot paused = rig.game.snapshot();
        rig.clock.nanos += 60_000_000_000L; loop.frame();
        equal(rig.game.snapshot().elapsedMillis(), paused.elapsedMillis(), "loop pause freezes wall time");
        key(input, loop, 'b'); equal(rig.game.snapshot().bombs(), paused.bombs(), "paused action ignored");
        key(input, loop, 'p'); rig.advance(300); key(input, loop, 'a'); rig.advance(300); key(input, loop, 's');
        key(input, loop, 'q'); check(!rig.game.snapshot().explosions().isEmpty(), "remote activation through queue");
        rig.advance(500); key(input, loop, 'p'); key(input, loop, 'r');
        equal(rig.game.snapshot().elapsedMillis(), 0L, "restart timer"); equal(rig.game.snapshot().player().lives(), 3, "restart lives");
        rig.advance(300_000); loop.frame(); equal(rig.game.state(), GameState.LOSE, "complete timeout run");
        equal(rig.game.snapshot().score(), 0, "lose has no timer/life bonus");
        check(TerminalRenderer.text(rig.game.snapshot()).contains("SKOR AKHIR"), "final score rendered");
        key(input, loop, 'r'); key(input, loop, 'p'); key(input, loop, 'm'); equal(rig.game.state(), GameState.MENU, "return to menu");
        Thread worker = new Thread(new KeyboardReader(new ByteArrayInputStream("3".getBytes(StandardCharsets.UTF_8)), input), "test-keyboard");
        worker.start(); worker.join(); loop.frame(); equal(rig.game.state(), GameState.EXIT, "concurrent keyboard exit");
        check(rendered.stream().anyMatch(s -> s.state() == GameState.PAUSED), "renderer received pause snapshot");
        Rig eof = new Rig(); CommandQueue<Integer> eofInput = new CommandQueue<>(4);
        GameLoop eofLoop = new GameLoop(eof.game, eofInput, s -> { });
        eofInput.put((int) '1'); eofInput.put(-1); eofLoop.frame(); eofLoop.frame();
        equal(eof.game.state(), GameState.EXIT, "EOF survives screen transition");
        System.out.println("IntegrationTest: PASS menu > help > play > remote > pause/resume > restart > timeout LOSE > menu > exit");
    }
    private static void key(CommandQueue<Integer> input, GameLoop loop, int c) throws Exception { input.put(c); loop.frame(); }
}
