# Catatan verifikasi

Lingkungan sesi: macOS, OpenJDK 25.0.3, `javac --release 17`, Python 3.14. Tidak ada dependency Java pihak ketiga. Pengujian menggunakan exception/assertion yang menghentikan proses dengan exit nonzero jika gagal; flag `-ea` tetap digunakan.

| Perintah | Hasil teramati |
|---|---|
| `make build` | JAR dibangun dengan seluruh lint dan warning sebagai error |
| `make test` | 21 kelompok aturan + integrasi + campaign lulus |
| `make integration` | Disediakan sebagai subset integrasi dan campaign dari `make test` |
| `make smoke` | PTY nyata, keyboard tanpa Enter, pause lebih lama dari fuse, restore termios: lulus |

Campaign memuat `config/` yang sama dengan aplikasi. Seed 17 menghasilkan WIN pada 103.600 ms, 3 nyawa, 196 detik utuh tersisa, skor 3260 = 5 x 100 + 500 + 3 x 100 + 196 x 10. Runner mengirim public command dan melihat snapshot, tanpa menulis state game. Tidak ada mode cheat pada produksi. Clock virtual mempercepat eksekusi suite; ini menguji aturan waktu simulasi. PTY menguji jalur waktu monotonic nyata dan adapter terminal.

Aturan diuji pada batas fuse 2.990/3.000 ms dan 4.490/4.500 ms, durasi flame 490/500 ms, invulnerability 1.490/1.500 ms, warning 990/1.000 ms, vulnerability 3.990/4.000 ms, dan cooldown 4.990/5.000 ms. Fixture unit secara sengaja mengisolasi objek, mengatur posisi, atau menonaktifkan musuh untuk menguji satu aturan. Hanya CampaignTest yang menjadi bukti level asli selesai.

Pengujian terminal pertama ditolak sandbox pada ioctl `stty` (`TIOCGETD: Operation not permitted`). Pengujian yang diizinkan di luar sandbox kemudian lulus. Ini pembatasan lingkungan pengujian, bukan fallback ke input berbasis Enter.

## Pemeriksaan UI terminal

- Menu 1 memulai permainan; menu 2 menampilkan petunjuk; tombol kembali mengembalikan menu; menu 3 mengakhiri program.
- Input menu yang tidak valid menampilkan pesan.
- WASD bergerak, pickup Remote terlihat; B memasang bom dan HUD menghitung slot.
- Pause membekukan tampilan, waktu, dan bomb selama 3,8 detik; resume meneruskan fuse tersisa.
- R mengembalikan kondisi awal, M kembali ke menu, Q pada pause keluar.
- Pengaturan `ICANON`, `ECHO`, serta seluruh atribut terminal sebelum dan setelah proses dibandingkan dan sama.
- Arrow sequence diuji lewat KeyboardReader dan seluruh arah di KeyBindings; panah kanan juga dipakai pada uji PTY nyata. Pemetaan jenis bomb dan Q diuji melalui engine/loop.

## Delivery gate desain yang relevan

- PASS, interaksi: kontrol di atas punya hasil nyata pada test; tidak ada tombol stub.
- PASS, arah visual: arena grid monospace mengikuti map PDF, HUD di atas, legenda di bawah; ENERGY 1 / RHYTHM 1 / MOTION 1. Fokus utama adalah posisi dan ancaman, aksen memakai simbol `!`/`^`/`*`, bukan warna yang bergantung tema terminal.
- PASS, kejujuran: status fitur bersumber pada test; nama/NIM/riwayat kosong diberi placeholder. Tidak ada klaim kontribusi fiktif.
- PASS, keterbacaan: ASCII dan tema/kontras bawaan terminal; semua kontrol memakai keyboard; pause menghentikan animasi game.
- PASS, build/run: lint tanpa warning, suite Java dan PTY lulus. Aturan web seperti mobile viewport, DOM/ARIA, hover, dan gambar hero tidak berlaku untuk CLI ini.
- PASS, PDF: buklet 10 halaman A5, diagram 2 halaman, pembagian tugas 1 halaman, log activity 2 halaman. Ukuran/jumlah halaman diperiksa dengan pypdf dan seluruh halaman dirender untuk inspeksi visual. Tumpang tindih kotak diagram pada draft diperbaiki sebelum PDF final.

Keterbatasan: satu seed campaign, satu platform terminal, belum ada uji manusia. Maven opsional belum dijalankan. Tidak mengklaim coverage persentase atau kompatibilitas semua terminal.
