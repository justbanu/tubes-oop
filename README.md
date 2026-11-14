# Nangor Blast: Stasiun Luar Angkasa (Map A)

Game Java CLI untuk satu pemain, berdasarkan seluruh 61 halaman **Copy of IF2010 Spesifikasi Tugas Besar.pdf**, versi 1.0. Layout 14 x 10 disalin dari halaman 39, aturan Draco dari halaman 30. Hanya map A yang diimplementasikan. Mekanisme lingkungannya adalah Reinforced Block; gate, air, lava, es, dan laser bukan bagian map A.

Tema tampilan adalah Stasiun Nangor: `#` hull, `D` peti kargo, `R` bulkhead berlapis, dan `X` airlock keluar. Ini hanya perubahan narasi; susunan 140 tile dan aturan map A tetap mengikuti spesifikasi.

## Build dan jalankan

Kebutuhan: JDK 17 atau lebih baru, `make`, dan terminal POSIX (macOS/Linux/WSL) dengan `stty` serta ANSI. Gunakan jendela minimal 80 kolom x 30 baris. Runtime tidak memakai dependency Java eksternal atau koneksi jaringan.

```sh
make build
make run
# Atau jalankan JAR dari direktori mana pun:
java -jar /path/ke/TubesOOP/build/nangor-blast.jar

# Reproduksi random dan muat konfigurasi eksternal:
java -jar build/nangor-blast.jar --seed 17 --config config
java -jar build/nangor-blast.jar --help
```

JAR menyertakan konfigurasi bawaan. `--config DIR` memuat `green-garden.map` dan `green-garden.properties` dari direktori tersebut; nama file dipertahankan agar pemuatan konfigurasi tetap kompatibel. File yang hilang/invalid menghasilkan pesan kesalahan dan exit code 1. Jangan mengubah susunan map yang dikumpulkan.

`make clean` membersihkan hasil build. `pom.xml` juga disediakan untuk impor IDE/Maven; jalur build dan pengujian yang telah diverifikasi adalah Makefile. `mvn package` bukan pengganti `make test`: suite pengujian ini memakai runner Java mandiri.

Terminal membaca tombol tanpa Enter dengan `stty -icanon -echo`. Pengaturan asli, kursor, dan layar dipulihkan ketika keluar normal, error, atau shutdown JVM. `--plain` tersedia untuk pipe/debug tanpa ANSI; mode itu tidak mengubah terminal ke input real-time. Pada Windows native gunakan WSL; adapter native Windows belum tersedia. Menahan tombol mengikuti auto-repeat keyboard sistem.

## Kontrol dan gameplay

| Konteks | Tombol | Aksi |
|---|---|---|
| Menu | `1` / Enter, `2` / H, `3` / Q | Start Game, How to Play, Exit |
| Bermain | WASD / panah | Gerak satu tile sesuai cooldown kecepatan |
| Bermain | B / Spasi | Pasang bomb terpilih |
| Bermain | `1`, `2` / E, `3` | Normal, Pumped, Remote |
| Bermain | Q | Detonasi seluruh Remote Bomb milik player |
| Bermain | P | Pause |
| Pause | P / C, R, M, Q | Lanjut, restart, menu, keluar |
| WIN / LOSE | R, M, Q | Restart, menu, keluar |
| Petunjuk | Tombol apa pun | Kembali ke menu |

Tujuan: kalahkan lima Normal Enemy dan Draco, kemudian masuk airlock `X` sebelum 300 detik habis. Player mulai dengan 3 nyawa, kapasitas 1 bomb, dan jangkauan 1. `P` adalah player, `p` player kebal, `E` musuh biasa, `B` Draco. `#`, `D`, `R` adalah hull, peti kargo, dan bulkhead reinforced. `o`, `O`, `@` menunjukkan Normal/Pumped/Remote Bomb; `!` peringatan boss, `^` jalur/area serangan, `*` ledakan.

Normal meledak setelah 3 detik; Pumped 4,5 detik dengan jangkauan +2 dan damage boss 2. Remote tidak memiliki fuse dan dibuka dengan mengambil `r` di sebelah spawn. Semua ledakan berlangsung 0,5 detik. Bomb yang sudah dipasang tidak berubah jenis saat pilihan diganti. Kapasitas baru kembali setelah ledakannya selesai. Player boleh meninggalkan bomb yang dipasangnya tetapi tidak bisa masuk kembali.

Draco memiliki 8 HP, fase kedua pada HP <= 4, dan empat pola: Fire Breath, Bite, Swoop, Wing Gust. Peringatan berlangsung 1 detik, lalu serangan dan cooldown 5 detik. Draco **hanya rentan pada 4 detik pertama cooldown setelah Fire Breath atau Swoop**. Arah/target dikunci saat warning. HUD menampilkan fase, HP, status, jenis serangan di pesan, dan `RENTAN`.

Power-up: `+` kapasitas (maksimal 3), `f` jangkauan (maksimal 5), `s` kecepatan (+50% awal, maksimal +100%), `r` Remote Detonator, `h` nyawa (maksimal 5). Setiap D memiliki peluang 30% menghasilkan tepat satu dari lima jenis, dengan pilihan seragam. Item tidak hancur oleh ledakan, tidak diambil enemy, dan efeknya bertahan setelah respawn. Restart menghapus semua peningkatan.

## Status fitur dan bukti

| Fitur | Status | Bukti pengujian |
|---|---|---|
| F00 Inisialisasi, validasi, restart | Berjalan | ukuran/simbol/spawn/rute/reachability/error konfigurasi; snapshot awal = setelah restart |
| F01 Player | Berjalan | gerak, cooldown, collision, respawn alternatif, batas invulnerability 1,5 detik |
| F02 Tiga bomb | Berjalan | fuse tepat, remote unlock/no fuse/detonate-all, kapasitas dan pemilihan jenis |
| F03 Explosion | Berjalan | 0,5 detik, chain iteratif, satu aktivasi/bomb, damage sekali/aktivasi |
| F04 Block | Berjalan | D/R menghentikan ray; tile yang dihancurkan tidak masuk ledakan; R hanya Pumped |
| F05 Enemy dan Draco | Berjalan | 2 patrol + 3 chase; deteksi 3 tile; warning/target terkunci/fase/durasi/cooldown/swoop |
| F06 Power-up | Berjalan | semua efek dan batas, threshold 30%, lima pilihan drop, Remote terjamin |
| F07 Map A | Berjalan | perbandingan 140 tile terhadap PDF; campaign asli mencapai exit |
| F08 Score | Berjalan | 100/enemy, 500/Draco; bonus WIN sekali; LOSE tanpa bonus |
| F09 Game Flow | Berjalan | menu/help/input invalid/PLAYING/PAUSED/WIN/LOSE/restart/menu/exit |
| Bonus | Tidak dikerjakan | Prioritas pada fitur wajib; tidak ada GUI, bomb kick, map maker, multiplayer |

## Pengujian

```sh
make test          # RulesTest + IntegrationTest + CampaignTest
make integration   # alur menu/LOSE dan campaign WIN
make smoke         # keyboard nyata melalui pseudo-terminal POSIX, perlu Python 3
```

Hasil yang telah dijalankan pada macOS, OpenJDK 25.0.3 (kompilasi `--release 17`):

- Build: `javac -Xlint:all -Werror` dan JAR berhasil tanpa warning.
- `RulesTest`: **21/21 kelompok uji lulus**. Fixture terisolasi dipakai untuk kondisi batas; bukan bukti kemenangan level.
- `IntegrationTest`: input queue + game loop + snapshot renderer melalui menu, help, remote, pause/resume, restart, timeout 300 detik, LOSE, menu, exit, dan EOF.
- `CampaignTest`: konfigurasi asli, seed 17, hanya public command dan pembacaan snapshot. **Lima enemy + Draco dikalahkan, exit dicapai pada 103.600 ms, 3 nyawa, sisa 196 detik, skor 3260.** Tidak ada teleport, pemberian item, perubahan HP, atau penghapusan musuh lewat fixture. Waktu simulasi dipercepat dengan clock uji; ini bukan rekaman manusia bermain selama 103 detik. Trace command ditulis ke `build/campaign-trace.txt`.
- `TerminalSmoke`: JAR nyata pada pseudo-terminal, tombol tanpa Enter, input invalid, help, gerak, bomb, pause 3,8 detik (lebih lama dari fuse), lanjut, restart, menu, exit, serta pengembalian konfigurasi `termios` lulus. Transkrip di `build/terminal-smoke.txt`.

Uji deterministik tidak membuktikan seluruh kemungkinan random; perilaku platform lain dan pengalaman bermain manusia tetap perlu dicoba saat demo. Tidak ada klaim performa atau coverage persentase yang tidak diukur. Lihat [laporan pengujian](doc/testing.md).

## Arsitektur, OOP, design pattern, dan SOLID

`GameEngine` adalah pemilik seluruh state game. Thread `keyboard-reader` hanya membaca byte dan menulis ke `CommandQueue<Integer>` berkapasitas 256. Thread `game-owner` membaca antrean, menjalankan command, memperbarui dunia, lalu merender snapshot immutable. Backpressure mencegah kehilangan command karena antrean penuh. Input lama dibuang saat berpindah layar, tetapi EOF/interupsi tetap dipertahankan. Akses engine dari thread lain ditolak.

Waktu produksi berasal dari `System.nanoTime` melalui `NanoClock`. Engine memakai langkah simulasi 10 ms; loop input/render berjalan sekitar 20 ms. Tidak ada `Timer`, `sleep`, atau wall clock terpisah untuk bomb/enemy/boss. Semua deadline mengacu ke waktu simulasi `now`, yang hanya bertambah saat PLAYING. Pause membekukan fuse, ledakan, movement cooldown, warning, attack, cooldown boss, invulnerability, dan timer permainan, termasuk sisa pecahan tick.

| Konsep | Lokasi penerapan nyata |
|---|---|
| Inheritance | `core/Entity.java` -> `Player`, `Enemy`, `Bomb`, `Explosion`; `Enemy` -> `NormalEnemy`, `Draco`; `Bomb` -> tiga subclass |
| Abstract/interface | `Entity`, `Enemy`, `Bomb`, `MovementStrategy`, `GameCommand`, `NanoClock`, `Renderer` |
| Polymorphism | `Enemy.update()` dalam tick; `Bomb.due()` pada fuse; `MovementStrategy.next()` pada normal enemy |
| Generics | `EntityStore<T extends Entity>` untuk enemy/bomb/explosion; `CommandQueue<T>` untuk produsen/konsumen |
| Exceptions | `MapValidationException` membungkus konfigurasi/I/O; owner guard; kegagalan terminal ditangani di `Main` |
| Collections | LinkedHashMap untuk urutan dunia; Set untuk tile/hit-ID; ArrayDeque untuk BFS/chain; List immutable pada snapshot |
| Concurrency | `runtime/KeyboardReader`, `CommandQueue`, `GameLoop`; tidak ada penulisan state game oleh input worker |
| Strategy | `MovementStrategy`, `PatrolStrategy`, `ChaseStrategy`; pemilihan perilaku di `NormalEnemy` |
| Factory Method | Implementasi `BombType.create()` pada tiap konstanta menghasilkan subclass Bomb dengan fuse berbeda |
| Command | `GameCommand.Move`, `Select`, `Action`; input diterjemahkan `KeyBindings`, dieksekusi pemilik state |
| SRP | Input (`runtime`), aturan (`core`), tampilan/terminal (`terminal`), dan parsing konfigurasi (`MapDefinition`) terpisah |
| OCP | Strategi movement baru dapat ditambahkan sebagai implementasi interface tanpa mengubah loop `NormalEnemy` |
| DIP | Engine bergantung pada abstraksi `NanoClock`; loop bergantung pada `Renderer`, memungkinkan clock/sink uji |
| LSP | Subclass Bomb memenuhi kontrak `due(now)`; Remote konsisten mengembalikan false, aktivasi manual tetap lewat detonate |

Diagram tersedia sebagai [Mermaid](doc/class-diagram.mmd) dan [PDF](doc/class-diagram.pdf). Tidak ada setter/debug command/cheat pada aplikasi produksi.

## Interpretasi untuk bagian yang tidak ditentukan PDF

1. Koordinat config nol-based; tampilan terminal dan PDF satu-based. Arena Draco mencakup kolom 8-13, baris 7-9, mengikuti area tenggara sekitar spawn boss. Tile `#` di dalam bounds tetap tidak dapat dilewati. Draco mulai serangan saat player pertama masuk bounds, kemudian tetap aktif; tidak ada gate arena tambahan.
2. Kecepatan awal player satu tile per 300 ms; musuh satu tile per 700 ms. Chase memakai jarak Manhattan <= 3 dan BFS dengan penghalang saat ini, kembali menuju waypoint patrol yang tertunda saat player keluar deteksi. Rute tetap bolak-balik di config. Enemy tidak saling menumpuk atau melewati bomb.
3. Player menabrak enemy atau enemy mencoba memasuki tile player dianggap kontak dan menimbulkan damage, tetapi keduanya tetap solid. Draco juga berhenti sebelum player kebal yang menghalangi Swoop. Swoop memberi damage pada tile yang benar-benar dilintasi Draco; `^` menunjukkan jalur ancamannya.
4. Fire Breath dan Swoop tersedia saat sejajar dengan player. Bite tersedia saat sejajar dan dalam 1 tile (2 pada fase kedua). Wing Gust dapat dilakukan dari posisi mana pun, termasuk jika player di luar areanya. Pemilihan acak tidak mengulang pola sebelumnya bila ada pilihan lain. Line attack berhenti pada semua block. Area gust mengabaikan tile block; serangan tidak menghancurkannya.
5. Fase diperbarui segera ketika HP <= 4; durasi/area serangan yang sudah dimulai tetap dikunci saat warning. Perubahan fase berlaku untuk pemilihan berikutnya. Boss tidak punya kekebalan tambahan antarbomb: dua aktivasi berbeda bisa mengenai boss pada cooldown yang sama.
6. Damage dihitung paling banyak sekali untuk setiap pasangan aktivasi dan target. Kontak baru dianggap kejadian baru; invulnerability global tetap membatasi damage setelah respawn. Damage yang ditolak karena kebal tidak mengonsumsi hak hit aktivasi itu.
7. Jangkauan bomb disalin ketika dipasang. Slot bomb tetap terpakai selama 0,5 detik ledakan. Beberapa bomb pada waktu yang sama diproses sesuai urutan pemasangan; bomb berikutnya dapat memanfaatkan block yang sudah dihancurkan sebelumnya. Chain memakai antrean dan ID sehingga berhingga.
8. Remote Detonator terjamin berada di tile kosong (kolom 3, baris 2), tanpa mengubah layout block/spawn. Drop acak dari D tetap independen; R tidak menghasilkan power-up.
9. Bonus waktu menggunakan detik utuh tersisa, dibulatkan ke bawah, sama dengan HUD. Jika nyawa/waktu habis bersamaan dengan syarat WIN, LOSE didahulukan. Random generator berlanjut setelah restart; kondisi dunia selalu kembali ke konfigurasi awal.
10. Validator memeriksa jalur dengan D/R yang dapat dihancurkan karena Pumped tersedia sejak awal, serta jalur langsung menuju Remote. Bukti bahwa layout bawaan dapat diselesaikan sebelum batas waktu berasal dari campaign, bukan dari klaim bahwa BFS menyelesaikan semua susunan map arbitrer.

## Struktur dan dokumen pengumpulan

```text
src/main/java/nangor/   core, runtime, terminal, Main
src/test/java/nangor/   unit, integration, campaign
config/                map A dan rute/arena/item
doc/                   buklet A5, diagram, pembagian tugas, log activity, laporan uji
scripts/               smoke test terminal dan generator PDF
Makefile               build, run, test, integration, smoke, docs, clean
pom.xml                konfigurasi impor/build Maven opsional
```

Empat PDF siap ditinjau: [buklet A5, 10 halaman](doc/buklet.pdf), [class diagram](doc/class-diagram.pdf), [pembagian tugas](doc/pembagian-tugas.pdf), [log activity](doc/log-activity.pdf). Sumber yang dapat diedit ada di `scripts/build_docs.py`, `doc/class-diagram.mmd`, dan dokumen Markdown. Untuk regenerasi: buat virtualenv, instal `scripts/requirements-docs.txt`, lalu `make docs`. Dependency PDF hanya untuk dokumentasi, bukan runtime game.

| Anggota | Fitur dan fokus rencana |
|---|---|
| Natasha Aurellia | F01 Player; F09 command dan transisi state |
| Banu Agil Triyanto | F02 Bomb; F03 Explosion; F04 Block; F06 Power-up |
| Muhammad Rizqi Azhar | F00 Inisialisasi/validasi; F05 Normal Enemy/Draco; F07 Map |
| Herlambang Setiaji Prabowo | F08 Score; F09 tampilan; integrasi, pengujian, dokumentasi |

F09 dibagi antara transisi game (Natasha) dan tampilan/uji alur (Herlambang). Source saat ini sudah mengimplementasikan F00-F09; tabel ini adalah rencana kepemilikan review/perbaikan, bukan klaim kontribusi historis. Rincian file, branch, urutan merge/push, dan persiapan repo resmi ada di [rencana pembagian tugas](doc/pembagian-tugas.md).

Nomor kelompok, NIM, asisten pembimbing, dan riwayat kontribusi tidak dicantumkan di sini. Tabel di atas adalah rencana pembagian tanggung jawab, bukan klaim kontribusi yang telah dilakukan.

Folder awal tidak memiliki `.git`. Tidak ada commit, push, undangan asisten, publikasi repository, atau GitHub release yang dibuat. Setelah identitas dan dokumen dilengkapi, kelompok dapat menginisialisasi Git dan melakukan pengumpulan sesuai PDF. Rujukan tanggal pengumpulan tetap berada pada PDF asli; aplikasi ini tidak melakukan pengumpulan otomatis.

## Kekurangan yang diketahui

- Real-time terminal diverifikasi pada macOS POSIX, belum pada Linux/WSL atau Windows native; butuh terminal ANSI dan `stty`.
- Pengujian campaign memakai satu seed yang direproduksi, bukan penelusuran seluruh kombinasi random atau uji kenyamanan oleh manusia.
- Identitas, kontribusi nyata anggota, riwayat pengembangan kelompok, dokumentasi asistensi, dan repository/release pengumpulan belum tersedia.
- Bonus tidak diimplementasikan. Tidak ada fitur wajib map A yang sengaja ditinggalkan sebagai stub.
