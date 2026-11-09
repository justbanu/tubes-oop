package nangor.runtime;

import java.util.concurrent.locks.LockSupport;
import nangor.core.GameEngine;
import nangor.core.GameState;
import nangor.terminal.Renderer;

public final class GameLoop {
    private final GameEngine engine;
    private final CommandQueue<Integer> input;
    private final Renderer renderer;
    public GameLoop(GameEngine engine, CommandQueue<Integer> input, Renderer renderer) {
        this.engine = engine; this.input = input; this.renderer = renderer;
    }

    public void frame() {
        engine.pulse();
        for (int i = 0; i < 64; i++) {
            Integer key = input.poll(); if (key == null) break;
            GameState before = engine.state(); engine.accept(KeyBindings.decode(key, before));
            if (engine.state() != before) {
                input.discardUnless(pending -> pending == -1 || pending == 3);
                break;
            }
        }
        renderer.render(engine.snapshot());
    }

    public void run() {
        while (engine.state() != GameState.EXIT && !Thread.currentThread().isInterrupted()) {
            frame(); LockSupport.parkNanos(20_000_000);
        }
    }
}
