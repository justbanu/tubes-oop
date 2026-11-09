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
        StringBuilder b = new StringBuilder("NANGOR BLAST | GREEN GARDEN (A)\n\n");
        if (s.state() == GameState.MENU) {
            b.append("1  Start Game\n2  How to Play\n3  Exit\n\n").append(s.message()).append('\n');
        } else if (s.state() == GameState.HELP) {
            b.append("Kalahkan 5 Normal Enemy dan Draco, lalu capai X.\n")
             .append("WASD / panah : bergerak     B / Spasi : pasang bomb\n")
             .append("1 Normal    2/E Pumped    3 Remote    Q Detonate\n")
             .append("P Pause     Saat pause: P lanjut, R restart, M menu\n\n")
             .append("Normal: 3 dtk. Pumped: 4,5 dtk, jangkauan +2.\n")
             .append("Remote: ambil r, lalu Q untuk meledakkan semua.\n")
             .append("R hanya hancur oleh Pumped. Ledakan menyakiti Deeper.\n")
             .append("Draco rentan 4 dtk setelah Fire Breath / Swoop.\n")
             .append("! peringatan 1 dtk; ^ serangan; * ledakan.\n")
             .append("p = kebal 1,5 dtk setelah respawn. Durasi game 300 dtk.\n\n")
             .append("Tekan tombol apa pun untuk kembali.\n");
        } else if (s.player() != null) {
            var p = s.player();
            b.append(String.format("%-7s Nyawa %d/5 | Skor %d | Waktu %03ds%n", s.state(), p.lives(), s.score(), s.secondsLeft()));
            b.append(String.format("Bomb %s %d/%d | Api %d | Speed %.1fx%n", p.selected(), p.occupiedSlots(), p.capacity(), p.range(), p.speed()));
            int hp = s.enemies().stream().filter(e -> e.kind().equals("DRACO")).mapToInt(GameSnapshot.EnemyView::health).sum();
            b.append(String.format("Draco %d/8 Fase %d | %s%s%n", hp, s.bossPhase(), s.bossStage(), s.vulnerable() ? " RENTAN" : ""));
            b.append("   01 02 03 04 05 06 07 08 09 10 11 12 13 14\n");
            for (int y = 0; y < s.board().size(); y++) {
                b.append(String.format("%02d ", y + 1));
                for (char tile : s.board().get(y).toCharArray()) b.append(' ').append(tile).append(' ');
                b.append('\n');
            }
            b.append("P Deeper E Enemy B Draco X Exit | # D R Block\n")
             .append("o Normal O Pumped @ Remote | ! Warning ^ Attack\n")
             .append("+ Bomb f Fire s Speed r Remote h Heart | * Blast\n")
             .append("WASD/panah gerak | B pasang | 1/2/3 bomb | Q ledak\n");
            if (s.state() == GameState.PAUSED) b.append("PAUSED: P lanjut | R restart | M menu | Q keluar\n");
            else if (s.state() == GameState.WIN || s.state() == GameState.LOSE)
                b.append("SKOR AKHIR: ").append(s.score()).append(" | R restart | M menu | Q keluar\n");
            else b.append("P pause | Remote: ").append(p.remote() ? "terbuka" : "ambil r").append('\n');
            b.append(s.message()).append('\n');
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
