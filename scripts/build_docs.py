#!/usr/bin/env python3
"""Generate the four submission PDFs; all unknown identities remain placeholders."""
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A5, A4, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "doc"
OUT.mkdir(exist_ok=True)
GREEN = colors.HexColor("#173F34")
INK = colors.HexColor("#182820")
PALE = colors.HexColor("#F3F5EF")
RUST = colors.HexColor("#98471F")


class Document:
    def __init__(self, name, size=A5):
        self.c = canvas.Canvas(str(OUT / name), pagesize=size)
        self.c.setTitle("Nangor Blast - " + name.removesuffix(".pdf"))
        self.c.setAuthor("[NAMA KELOMPOK - BELUM DIISI]")
        self.w, self.h = size
        self.page = 0
        self.y = 0

    def start(self, section, title):
        if self.page:
            self.c.showPage()
        self.page += 1
        c = self.c
        c.setFillColor(PALE); c.rect(0, 0, self.w, self.h, fill=1, stroke=0)
        c.setFillColor(GREEN); c.rect(0, self.h - 54, self.w, 54, fill=1, stroke=0)
        c.setFillColor(colors.white); c.setFont("Helvetica", 9)
        c.drawString(30, self.h - 22, "NANGOR BLAST / GREEN GARDEN")
        c.setFont("Helvetica", 8); c.drawRightString(self.w - 30, self.h - 40, section)
        c.setFillColor(INK); c.setFont("Helvetica-Bold", 19)
        c.drawString(30, self.h - 88, title)
        c.setStrokeColor(GREEN); c.setLineWidth(0.5); c.line(30, 32, self.w - 30, 32)
        c.setFont("Helvetica", 8); c.drawString(30, 19, "IF2010 / Map A / Identitas masih placeholder")
        c.drawRightString(self.w - 30, 19, str(self.page))
        self.y = self.h - 110

    def para(self, text, size=10, gap=10, bold=False, color=INK):
        style = ParagraphStyle("body", fontName="Helvetica-Bold" if bold else "Helvetica",
                               fontSize=size, leading=size * 1.45, textColor=color)
        p = Paragraph(escape(text).replace("\n", "<br/>"), style)
        _, height = p.wrap(self.w - 60, self.h)
        if self.y - height < 45:
            raise ValueError(f"Overflow on page {self.page}: {text[:50]}")
        p.drawOn(self.c, 30, self.y - height)
        self.y -= height + gap

    def mono(self, lines, size=11, leading=16):
        self.c.setFillColor(INK); self.c.setFont("Courier", size)
        for line in lines:
            if self.y < 48:
                raise ValueError("Monospace overflow")
            self.c.drawString(34, self.y, line); self.y -= leading
        self.y -= 8

    def save(self):
        self.c.save()


def box(c, x, y, w, title, lines=(), height=58):
    c.setFillColor(colors.white); c.setStrokeColor(GREEN); c.setLineWidth(0.8)
    c.rect(x, y, w, height, fill=1, stroke=1)
    c.setFillColor(GREEN); c.setFont("Helvetica-Bold", 9)
    c.drawCentredString(x + w / 2, y + height - 16, title)
    if lines:
        c.line(x, y + height - 23, x + w, y + height - 23)
        c.setFillColor(INK); c.setFont("Helvetica", 8)
        for i, line in enumerate(lines):
            c.drawString(x + 6, y + height - 36 - 11 * i, line)


def arrow(c, x1, y1, x2, y2, inheritance=False, dashed=False):
    import math
    c.setStrokeColor(GREEN); c.setFillColor(colors.white if inheritance else GREEN)
    c.setLineWidth(0.8); c.setDash(3, 2) if dashed else c.setDash()
    c.line(x1, y1, x2, y2); c.setDash()
    a = math.atan2(y2 - y1, x2 - x1)
    length, width = (9, 4) if inheritance else (6, 2.5)
    p = c.beginPath(); p.moveTo(x2, y2)
    p.lineTo(x2 - length * math.cos(a) + width * math.sin(a), y2 - length * math.sin(a) - width * math.cos(a))
    p.lineTo(x2 - length * math.cos(a) - width * math.sin(a), y2 - length * math.sin(a) + width * math.cos(a))
    p.close(); c.drawPath(p, fill=1, stroke=1)


def domain_diagram(d):
    c = d.c
    # On A4 landscape, every class label remains 9pt or larger.
    box(c, 345, 432, 150, "Entity (abstract)", ["id: long", "position: Position"], 66)
    children = [(35, "Player"), (230, "Enemy (abstract)"), (425, "Bomb (abstract)"), (620, "Explosion")]
    for x, title in children:
        box(c, x, 315, 165, title,
            {"Player": ["lives, capacity, range", "collect(PowerUpType)"],
             "Enemy (abstract)": ["health, scoreValue", "update(GameEngine)"],
             "Bomb (abstract)": ["owner, range, armed", "due(now): boolean"],
             "Explosion": ["tiles: Set<Position>", "damaged: Set<Long>"]}[title], 70)
        arrow(c, x + 82, 385, 420, 432, inheritance=True)
    for x, title in [(205, "NormalEnemy"), (320, "Draco")]:
        box(c, x, 213, 105, title)
        arrow(c, x + 52, 271, 312, 315, inheritance=True)
    for x, title in [(450, "NormalBomb"), (565, "PumpedBomb"), (680, "RemoteBomb")]:
        box(c, x, 213, 105, title)
        arrow(c, x + 52, 271, 507, 315, inheritance=True)
    box(c, 35, 95, 160, "MovementStrategy", ["interface: next(enemy, game)"], 60)
    arrow(c, 257, 213, 160, 155, dashed=True)
    box(c, 225, 95, 145, "PatrolStrategy"); arrow(c, 225, 124, 195, 124, inheritance=True, dashed=True)
    box(c, 400, 95, 145, "ChaseStrategy")
    c.setDash(3, 2); c.line(472, 95, 472, 75); c.line(472, 75, 115, 75); c.setDash()
    arrow(c, 115, 75, 115, 95, inheritance=True, dashed=True)
    box(c, 590, 95, 190, "BombType (enum)", ["create(): Bomb", "Factory Method per type"], 60)
    for target_x in [502, 617, 732]:
        arrow(c, 685, 155, target_x, 213, dashed=True)


def runtime_diagram(d):
    c = d.c
    box(c, 35, 400, 165, "KeyboardReader", ["input worker", "reads bytes only"], 72)
    box(c, 265, 400, 190, "CommandQueue<T>", ["ArrayBlockingQueue<T>", "bounded FIFO / backpressure"], 72)
    box(c, 520, 400, 220, "GameLoop", ["owner thread", "frame(): pulse, commands, render"], 72)
    arrow(c, 200, 436, 265, 436); arrow(c, 455, 436, 520, 436)
    box(c, 520, 270, 220, "GameEngine", ["accept(GameCommand), pulse()", "owns state; snapshot()"], 72)
    arrow(c, 630, 400, 630, 342)
    box(c, 265, 270, 190, "GameCommand", ["interface: execute(GameEngine)", "Move / Select / Action"], 72)
    arrow(c, 455, 306, 520, 306)
    box(c, 35, 270, 165, "KeyBindings", ["decode(key, GameState)"], 72)
    arrow(c, 200, 306, 265, 306)
    box(c, 520, 140, 220, "GameSnapshot", ["immutable records/collections", "board + player + enemies + HUD"], 72)
    arrow(c, 630, 270, 630, 212)
    box(c, 265, 140, 190, "Renderer", ["interface: render(snapshot)", "TerminalRenderer implements"], 72)
    arrow(c, 520, 176, 455, 176)
    box(c, 35, 140, 165, "NanoClock", ["interface: nanoTime()", "system / manual test clock"], 72)
    arrow(c, 200, 212, 520, 270, dashed=True)
    c.setFillColor(INK); c.setFont("Helvetica", 10)
    c.drawString(35, 92, "GameEngine composes Player, EntityStore<Enemy/Bomb/Explosion>, blocks, power-ups.")
    c.drawString(35, 75, "MapDefinition validates configuration; Main owns RawTerminal lifecycle and wires adapters.")
    c.drawString(35, 58, "Legend: solid arrow = data/use; dashed = dependency; hollow triangle = inheritance/implementation.")


def booklet():
    d = Document("buklet.pdf")
    d.start("01 / COVER", "Nangor Blast")
    d.para("Green Garden / Map A", 17, 24, bold=True, color=GREEN)
    d.para("Game Java CLI dengan bomb, lima musuh, dan Draco. Dibangun dari spesifikasi IF2010 versi 1.0, dibaca utuh 61 halaman.", 12, 24)
    d.para("Kelompok: [NAMA KELOMPOK]\nNomor: [NOMOR KELOMPOK]", 11, 16, bold=True)
    for i in range(1, 6):
        d.para(f"[NAMA {i}] / [NIM {i}]" + (" (opsional, anggota kelima)" if i == 5 else ""), 10, 5)
    d.para("Asisten: [NAMA ASISTEN]", 10, 20)
    d.para("Identitas, kontribusi anggota, dan riwayat kelompok belum diberikan. Placeholder harus diisi sebelum pengumpulan.", 10, color=RUST)

    d.start("02 / USER MANUAL", "Mulai bermain")
    d.para("Kebutuhan: JDK 17+, make, terminal POSIX/ANSI dengan stty. Ukuran terminal disarankan 80 x 30. Tidak ada dependency Java eksternal.")
    d.mono(["make build", "make run", "make test", "make smoke"], 12, 20)
    d.para("Menu: 1 Start Game, 2 How to Play, 3 Exit. Pemain memilih lewat keyboard tanpa Enter.", bold=True)
    d.para("WASD / panah: gerak\nB / Spasi: pasang bomb\n1: Normal, 2 / E: Pumped, 3: Remote\nQ: ledakkan seluruh Remote Bomb\nP: pause")
    d.para("Saat pause: P lanjut, R restart, M menu, Q keluar. Saat WIN/LOSE: R restart, M menu, Q keluar.")
    d.para("Player mulai dengan 3 nyawa, kapasitas 1, jangkauan 1, dan waktu 300 detik. Kalahkan semua musuh lalu capai X. Nyawa atau waktu habis menyebabkan LOSE.")

    d.start("03 / MAP", "Reruntuhan Green Garden")
    d.para("Susunan 140 tile mengikuti halaman 39 PDF. Ini satu-satunya map. R adalah struktur batu kuno yang hanya hancur oleh Pumped Bomb.")
    d.mono((ROOT / "config/green-garden.map").read_text().splitlines(), 15, 20)
    d.para("# permanen, D destructible, R reinforced, P player, E enemy, B Draco, X exit. Tile titik dapat dilalui.", 10)
    d.para("Remote Detonator terjamin diletakkan pada tile kosong di kolom 3, baris 2. Koordinat config nol-based; diagram dan HUD satu-based.", 10)
    d.para("Arena Draco: kolom 8-13, baris 7-9. Batas ini adalah interpretasi karena PDF tidak merinci arena. Tidak ada gate/hazard map lain.", 10)

    d.start("04 / COMBAT", "Bomb dan Draco")
    d.para("Normal: fuse 3 detik, range player, 1 damage boss. Pumped: fuse 4,5 detik, range +2 (maksimal 7), 2 damage boss. Remote: dibuka oleh r, tanpa fuse, 1 damage boss.")
    d.para("Ledakan 0,5 detik menyebar silang. # menghentikan ray. D dan R juga menghentikannya; tile block yang hancur tidak termasuk ledakan. Chain memakai antrean, satu aktivasi per bomb.")
    d.para("Player respawn setelah damage dan kebal 1,5 detik. Satu aktivasi tidak bisa memberi damage berulang pada target yang sama. Slot bomb kembali setelah ledakan selesai.")
    d.para("Draco: 8 HP, fase kedua pada HP <= 4. Warning 1 detik mengunci target dan arah. Fire Breath / Swoop: 1 detik, menjadi 0,5 pada fase kedua. Bite: 0,75 detik, range 1 menjadi 2. Wing Gust: 1 detik, radius 1 menjadi 2.")
    d.para("Cooldown selalu 5 detik. Hanya 4 detik pertama setelah Fire Breath atau Swoop yang rentan. Peringatan ! dan serangan ^ memberi waktu menghindar.", bold=True, color=GREEN)

    d.start("05 / POWER & SCORE", "Bertahan dan menyelesaikan")
    d.para("Power-up: + kapasitas maksimal 3; f range maksimal 5; s kecepatan +50% awal, maksimal +100%; r membuka Remote; h nyawa maksimal 5.")
    d.para("Setiap D memiliki peluang 30% menghasilkan tepat satu jenis acak. Item tidak rusak oleh ledakan atau diambil enemy. Efek tetap setelah respawn dan kembali awal saat restart.")
    d.para("Dua musuh patrol mengikuti rute tetap bolak-balik. Tiga chase mendeteksi player dalam jarak Manhattan 3 tile, lalu mencari jalur melalui BFS. Ketika keluar jangkauan, musuh kembali ke rute.")
    d.para("Skor: 100 per Normal Enemy, 500 untuk Draco. Hanya WIN menambah 100 per nyawa dan 10 per detik utuh tersisa. Jika timer/nyawa habis bersamaan dengan exit, LOSE didahulukan.")
    d.para("Kecepatan awal player: 300 ms per tile; musuh: 700 ms per tile. Semua keputusan ambigu beserta alasannya dijabarkan dalam README.")

    d.start("06 / DOMAIN MODEL", "Struktur objek")
    d.mono(["Entity (abstract)", "  + Player", "  + Enemy (abstract)", "  |   + NormalEnemy", "  |   + Draco", "  + Bomb (abstract)", "  |   + NormalBomb", "  |   + PumpedBomb", "  |   + RemoteBomb", "  + Explosion"], 12, 19)
    d.para("NormalEnemy menggunakan MovementStrategy. PatrolStrategy dan ChaseStrategy mengimplementasikan next(). Draco menjalankan AttackPattern melalui WARNING, ACTIVE, dan COOLDOWN.")
    d.para("EntityStore<T extends Entity> menyimpan koleksi berdasarkan ID. Explosion menyimpan Set tile dan Set ID yang sudah terkena damage. Factory Method BombType.create() membuat subclass yang tepat.")
    d.para("Diagram formal dua halaman tersedia di doc/class-diagram.pdf; sumber lengkap dapat diedit di doc/class-diagram.mmd.", 9, color=GREEN)

    d.start("07 / ARCHITECTURE", "Satu pemilik state")
    d.mono(["keyboard-reader", "    | byte keyboard", "CommandQueue<Integer>", "    | FIFO bounded", "game-owner / GameLoop", "    | GameCommand.execute()", "GameEngine + NanoClock", "    | immutable snapshot", "TerminalRenderer"], 12, 21)
    d.para("Thread input tidak menyentuh dunia. Engine menolak akses dari thread lain. Waktu monotonic System.nanoTime diubah menjadi tick 10 ms, hanya maju saat PLAYING. Pause membekukan semua deadline.")
    d.para("Pattern: Strategy untuk movement, Factory Method untuk bomb, Command untuk aksi. SOLID: SRP memisah input/aturan/tampilan; OCP pada strategi; DIP melalui NanoClock dan Renderer.")
    d.para("Konsep wajib inheritance, abstract/interface, polymorphism, generics, exception, collection, dan concurrency dipetakan ke file konkret di README.", 9)

    d.start("08 / VERIFICATION", "Bukti perilaku")
    d.para("Build menggunakan javac --release 17 -Xlint:all -Werror. Hasil: berhasil tanpa warning pada OpenJDK 25.0.3 / macOS.")
    d.para("RulesTest: 21 kelompok aturan lulus, termasuk batas waktu fuse, flame, invulnerability, vulnerability, chain, collision, power-up, score, dan restart.")
    d.para("IntegrationTest: menu, help, input invalid, remote, pause/resume, restart, timeout 300 detik sampai LOSE, menu, exit, serta EOF.")
    d.para("CampaignTest: map asli, seed 17, public command saja. WIN pada 103,6 detik simulasi, 3 nyawa, sisa 196 detik, skor 3260. Lima enemy dan Draco benar-benar dikalahkan. Tidak ada modifikasi state fixture dalam campaign.", bold=True, color=GREEN)
    d.para("TerminalSmoke: JAR nyata dalam PTY, keyboard tanpa Enter, pause 3,8 detik, resume, restart, menu, exit dan restore termios lulus.")
    d.para("Ini bukan uji manusia atau bukti seluruh seed/platform. Laporan dan batas verifikasi ada di doc/testing.md.", 9, color=RUST)

    d.start("09 / TEAM", "Peran dan pembagian tugas")
    d.para("Usulan tanggung jawab berikut belum disahkan kelompok. Tidak ada atribusi kontribusi aktual tanpa data anggota.", color=RUST)
    roles = ["Model, player, input dan game loop", "Bomb, explosion, block dan power-up", "Enemy, Draco, map dan validasi", "Terminal, integrasi, QA dan dokumentasi", "Review/pengujian/koordinasi dokumen (opsional)"]
    for i, role in enumerate(roles, 1):
        d.para(f"[NAMA {i}] / [NIM {i}]", 11, 3, bold=True)
        d.para(role + ". Kontribusi aktual: [BELUM DIBERIKAN].", 10, 15)
    d.para("Untuk kelompok empat orang, hapus anggota kelima dan bagikan peran review. Dokumen terpisah: pembagian-tugas.pdf dan log-activity.pdf.", 9)

    d.start("10 / DEVELOPMENT", "Proses dan tindak lanjut")
    d.para("Sesi implementasi dimulai dari folder yang hanya berisi PDF. Spesifikasi dibaca utuh, layout diverifikasi visual, lalu dibuat konfigurasi, model, engine deterministik, adapter terminal, dan test.")
    d.para("Build pertama mengungkap warning subclass Bomb di file yang sama; masing-masing dipisahkan. Review mengoreksi EOF pada transisi layar dan damage Swoop. Pengujian campaign menggunakan aksi nyata untuk memeriksa ketercapaian WIN.")
    d.para("Rancangan awal kelompok: [BELUM DIBERIKAN]. Perubahan rancangan kelompok dan alasan: [BELUM DIBERIKAN]. Rapat, ideasi, foto, asistensi, dan tanggal kontribusi: [BELUM DIBERIKAN].", color=RUST)
    d.para("Catatan sesi bantuan bukan log activity anggota. Isi riwayat asli pada log-activity.md dan sesuaikan sumber generator sebelum membuat PDF pengumpulan final.")
    d.para("Batas saat ini: native Windows belum didukung, uji terminal baru macOS, satu seed campaign, bonus belum dikerjakan. Tidak ada repository GitHub, commit, undangan asisten, atau release yang dibuat.")
    d.para("Sumber persyaratan: Copy of IF2010 Spesifikasi Tugas Besar.pdf, versi 1.0, halaman 1-61. README memuat seluruh asumsi dan pemetaan implementasi.", 9)
    assert d.page == 10
    d.save()


def diagrams():
    d = Document("class-diagram.pdf", landscape(A4))
    d.start("MODEL", "Class diagram / domain")
    domain_diagram(d)
    d.start("RUNTIME", "Class diagram / alur dan dependensi")
    runtime_diagram(d)
    d.save()


def assignments():
    d = Document("pembagian-tugas.pdf", A4)
    d.start("MILESTONE 1", "Pembagian tugas kelompok")
    d.para("[NAMA KELOMPOK] / [NOMOR KELOMPOK]\nAsisten: [NAMA ASISTEN]", 12, 24, bold=True)
    d.para("Usulan untuk dikonfirmasi. Semua nama/NIM dan kontribusi aktual belum diberikan; dokumen ini tidak mengklaim anggota telah menulis bagian kode tertentu.", 11, 24, color=RUST)
    roles = ["Model, player, input dan game loop", "Bomb, explosion, block dan power-up", "Normal enemy, Draco, map dan validasi", "Terminal, menu, integrasi, QA dan dokumentasi", "Review/pengujian/koordinasi dokumen, jika ada anggota kelima"]
    for i, role in enumerate(roles, 1):
        d.para(f"[NAMA {i}] / [NIM {i}]" + (" (opsional)" if i == 5 else ""), 13, 4, bold=True)
        d.para("Tanggung jawab usulan: " + role + ".\nKontribusi aktual: [BELUM DIBERIKAN]", 11, 24)
    d.para("Kelompok terdiri dari 4-5 orang sesuai spesifikasi. Sesuaikan tabel berdasarkan kesepakatan dan riwayat sebenarnya. Dokumen pendamping: class-diagram.pdf.", 10)
    d.save()


def activity():
    d = Document("log-activity.pdf", A4)
    for page_members in [(1, 2, 3), (4, 5)]:
        d.start("MILESTONE 2", "Log activity anggota")
        d.para("[NAMA KELOMPOK] / [NOMOR KELOMPOK]. Isi dari pekerjaan nyata. Baris placeholder berikut bukan riwayat pengerjaan.", 11, 20, color=RUST)
        for i in page_members:
            d.para(f"[NAMA {i}] / [NIM {i}]" + (" (opsional)" if i == 5 else ""), 12, 8, bold=True)
            c = d.c; x = 30; top = d.y; widths = [115, 220, d.w - 60 - 335]
            headers = ["Waktu", "Kegiatan", "Hasil"]
            for row in range(4):
                xx = x; height = 30
                for col, width in enumerate(widths):
                    c.setFillColor(GREEN if row == 0 else colors.white)
                    c.setStrokeColor(GREEN); c.rect(xx, top - height, width, height, fill=1, stroke=1)
                    c.setFillColor(colors.white if row == 0 else INK); c.setFont("Helvetica-Bold" if row == 0 else "Helvetica", 9)
                    label = headers[col] if row == 0 else ["[TANGGAL]", "[KEGIATAN AKTUAL]", "[HASIL AKTUAL]"][col]
                    c.drawString(xx + 8, top - 19, label); xx += width
                top -= height
            d.y = top - 28
        if page_members[-1] == 5:
            d.para("Hapus anggota kelima jika kelompok berisi empat orang. Tambahkan baris sesuai kebutuhan. Lampirkan bukti asistensi/rapat yang benar-benar tersedia; jangan membuat tanggal, aktivitas, atau hasil fiktif.", 11)
        d.para("Catatan sesi bantuan implementasi disimpan terpisah di development-notes.md dan tidak boleh otomatis diatribusikan kepada anggota.", 9)
    d.save()


if __name__ == "__main__":
    booklet(); diagrams(); assignments(); activity()
    from pypdf import PdfReader
    for file in ["buklet.pdf", "class-diagram.pdf", "pembagian-tugas.pdf", "log-activity.pdf"]:
        reader = PdfReader(OUT / file)
        print(f"{file}: {len(reader.pages)} pages")
    reader = PdfReader(OUT / "buklet.pdf")
    assert len(reader.pages) <= 15
    for page in reader.pages:
        assert abs(float(page.mediabox.width) - A5[0]) < 1 and abs(float(page.mediabox.height) - A5[1]) < 1
