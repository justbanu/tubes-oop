package nangor.runtime;

import java.io.IOException;
import java.io.InputStream;

public final class KeyboardReader implements Runnable {
    private final InputStream input;
    private final CommandQueue<Integer> queue;
    public KeyboardReader(InputStream input, CommandQueue<Integer> queue) { this.input = input; this.queue = queue; }

    @Override public void run() {
        try {
            int escape = 0;
            while (!Thread.currentThread().isInterrupted()) {
                int c = input.read();
                if (c == -1) { queue.put(-1); return; }
                if (c == 27) { escape = 1; continue; }
                if (escape == 1 && (c == '[' || c == 'O')) { escape = 2; continue; }
                if (escape == 2) {
                    c = switch (c) { case 'A' -> KeyBindings.UP; case 'B' -> KeyBindings.DOWN;
                        case 'C' -> KeyBindings.RIGHT; case 'D' -> KeyBindings.LEFT; default -> 0; };
                }
                escape = 0;
                if (c != 0) queue.put(c);
            }
        } catch (IOException e) {
            System.getLogger(KeyboardReader.class.getName()).log(System.Logger.Level.ERROR, "keyboard_read_failed", e);
            try { queue.put(-1); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
