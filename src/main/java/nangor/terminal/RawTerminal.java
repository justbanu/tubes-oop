package nangor.terminal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** POSIX terminal adapter. The exact original stty state is restored on every exit. */
public final class RawTerminal implements AutoCloseable {
    private final String original;
    private final Thread restoreHook;
    private boolean closed;

    private RawTerminal(String original) {
        this.original = original;
        restoreHook = new Thread(this::restore, "terminal-restore");
        Runtime.getRuntime().addShutdownHook(restoreHook);
    }

    public static RawTerminal open() throws IOException, InterruptedException {
        if (System.console() == null) throw new IOException("Butuh terminal interaktif. Jalankan make run pada Terminal; gunakan --plain untuk input pipe.");
        String original = stty("-g").strip();
        if (original.isEmpty()) throw new IOException("Tidak dapat membaca konfigurasi terminal");
        RawTerminal terminal = new RawTerminal(original);
        try { stty("-icanon", "-echo", "min", "1", "time", "0"); }
        catch (IOException | InterruptedException e) { terminal.close(); throw e; }
        System.out.print("\033[?1049h\033[?25l"); System.out.flush();
        return terminal;
    }

    private static String stty(String... options) throws IOException, InterruptedException {
        String[] command = new String[options.length + 1]; command[0] = "stty";
        System.arraycopy(options, 0, command, 1, options.length);
        Process process = new ProcessBuilder(command).redirectInput(new File("/dev/tty")).redirectErrorStream(true).start();
        if (!process.waitFor(2, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("stty timeout"); }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new IOException("stty: " + output.strip());
        return output;
    }

    private synchronized void restore() {
        if (closed) return;
        closed = true;
        // Restore even when the game thread was interrupted during shutdown.
        boolean interrupted = Thread.interrupted();
        try { stty(original); }
        catch (IOException e) { System.err.println("Gagal memulihkan terminal. Jalankan stty sane: " + e.getMessage()); }
        catch (InterruptedException e) { interrupted = true; }
        finally {
            System.out.print("\033[0m\033[?25h\033[?1049l"); System.out.flush();
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    @Override public void close() {
        restore();
        try { Runtime.getRuntime().removeShutdownHook(restoreHook); } catch (IllegalStateException ignored) { /* JVM shutdown already owns the hook. */ }
    }
}
