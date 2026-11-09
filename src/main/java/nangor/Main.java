package nangor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import nangor.core.GameEngine;
import nangor.core.MapDefinition;
import nangor.core.MapValidationException;
import nangor.runtime.CommandQueue;
import nangor.runtime.GameLoop;
import nangor.runtime.KeyboardReader;
import nangor.terminal.RawTerminal;
import nangor.terminal.TerminalRenderer;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        try { run(args); }
        catch (IOException | MapValidationException | IllegalArgumentException e) {
            System.err.println("Nangor Blast: " + e.getMessage()); System.exit(1);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void run(String[] args) throws IOException, MapValidationException, InterruptedException {
        boolean plain = false; Path config = null; long seed = System.nanoTime();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--plain" -> plain = true;
                case "--config" -> { if (++i == args.length) throw new IllegalArgumentException("--config memerlukan direktori"); config = Path.of(args[i]); }
                case "--seed" -> { if (++i == args.length) throw new IllegalArgumentException("--seed memerlukan angka"); seed = Long.parseLong(args[i]); }
                case "--help" -> { System.out.println("java -jar build/nangor-blast.jar [--config DIR] [--seed NUMBER] [--plain]"); return; }
                default -> throw new IllegalArgumentException("Opsi tidak dikenal: " + args[i]);
            }
        }
        MapDefinition map = MapDefinition.load(config);
        Thread.currentThread().setName("game-owner");
        GameEngine engine = new GameEngine(map, new Random(seed), System::nanoTime);
        CommandQueue<Integer> queue = new CommandQueue<>(256);
        Thread keyboard = new Thread(new KeyboardReader(System.in, queue), "keyboard-reader");
        keyboard.setDaemon(true);
        RawTerminal terminal = plain ? null : RawTerminal.open();
        try {
            keyboard.start();
            new GameLoop(engine, queue, new TerminalRenderer(System.out, !plain)).run();
        } finally {
            keyboard.interrupt();
            if (terminal != null) terminal.close();
        }
    }
}
