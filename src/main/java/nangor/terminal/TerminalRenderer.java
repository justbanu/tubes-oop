package nangor.terminal;

import java.io.PrintStream;
import nangor.core.GameSnapshot;
import nangor.core.GameState;

public final class TerminalRenderer implements Renderer {
    private final PrintStream out;
    private final boolean ansi;
    private String previous = "";
    public TerminalRenderer(PrintStream out, boolean ansi) { this.out = out; this.ansi = ansi; }

    public static String text(GameSnapshot s) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("%-48s GREEN GARDEN / MAP A%n", "NANGOR BLAST"));
        if (s.state() == GameState.MENU) {
            b.append("MENU\n\n")
             .append("  1  Start Game\n  2  How to Play\n  3  Exit\n\n")
             .append("Pesan: ").append(s.message()).append('\n');
        } else if (s.state() == GameState.HELP) {
            b.append("HOW TO PLAY\n\n")
             .append("Kalahkan 5 Normal Enemy dan Draco, lalu capai X dalam 300 detik.\n\n")
             .append("KONTROL\n")
             .append("  WASD / panah  Gerak                 B / Spasi  Pasang bomb\n")
             .append("  1 Normal      2 / E Pumped          3 Remote (setelah ambil r)\n")
             .append("  Q Detonate    P Pause    Saat jeda: P lanjut, R restart, M menu, Q keluar\n\n")
             .append("BOMB DAN BOSS\n")
             .append("  Normal: 3 dtk. Pumped: 4,5 dtk, jangkauan +2, merusak R.\n")
             .append("  Remote: ambil r, lalu Q untuk meledakkan semua bomb remote.\n")
             .append("  Draco rentan 4 dtk setelah Fire Breath atau Swoop.\n")
             .append("  Ledakan menyakiti Deeper; setelah respawn kebal 1,5 dtk.\n\n")
             .append("LEGENDA MAP\n")
             .append("  P Deeper   p kebal    E Enemy    B Draco    X Exit     . jalan\n")
             .append("  # dinding  D rapuh    R kuat (hanya Pumped)\n")
             .append("  o Normal   O Pumped   @ Remote   * ledakan\n")
             .append("  ! warning  ^ serangan Draco\n")
             .append("  + Bomb Up  f Fire Up  s Speed Up  r Remote  h Heart\n\n")
             .append("Tekan tombol apa pun untuk kembali ke menu.\n");
        } else if (s.player() != null) {
            var p = s.player();
            int hp = s.enemies().stream().filter(e -> e.kind().equals("DRACO")).mapToInt(GameSnapshot.EnemyView::health).sum();
            b.append(String.format("%-9s Draco %d/8 | Fase %d | %s%s%n", s.state(), hp,
                    s.bossPhase(), s.bossStage(), s.vulnerable() ? " | RENTAN" : ""));
            b.append(String.format("Nyawa %d/5  |  %s %d  |  Waktu %03ds%n", p.lives(),
                    s.state() == GameState.WIN || s.state() == GameState.LOSE ? "SKOR AKHIR" : "Skor",
                    s.score(), s.secondsLeft()));
            b.append(String.format("Bomb %-6s %d/%d  |  Api %d  |  Speed %.1fx  |  Remote %s%n",
                    p.selected(), p.occupiedSlots(), p.capacity(), p.range(), p.speed(),
                    p.remote() ? "siap" : "terkunci"));
            b.append('\n');
            b.append("    ");
            for (int x = 1; x <= 14; x++) b.append(String.format("%02d ", x));
            b.append('\n');
            for (int y = 0; y < s.board().size(); y++) {
                b.append(String.format("%02d  ", y + 1));
                for (char tile : s.board().get(y).toCharArray()) b.append(tile).append("  ");
                b.append('\n');
            }
            b.append('\n');
            if (s.state() == GameState.PAUSED)
                b.append("PAUSED: P lanjut  |  R restart  |  M menu  |  Q keluar\n");
            else if (s.state() == GameState.WIN || s.state() == GameState.LOSE)
                b.append("R restart  |  M menu  |  Q keluar\n");
            else b.append("WASD/panah gerak  |  B pasang  |  1/2/3 bomb  |  Q ledak  |  P pause\n");
            b.append("Pesan: ").append(s.message()).append('\n');
        }
        return b.toString();
    }

    @Override public void render(GameSnapshot snapshot) {
        String frame = text(snapshot);
        if (frame.equals(previous)) return;
        if (ansi) {
            out.print("\033[H");
            for (String line : frame.split("\n", -1)) out.print(line + "\033[K\r\n");
            out.print("\033[J");
        } else { out.println(frame); }
        out.flush(); previous = frame;
    }
}
