# Rencana pembagian tugas dan workflow Milestone 1

Pembagian berikut adalah rencana kepemilikan implementasi, review, dan integrasi untuk fitur F00-F09. Ini bukan klaim kontribusi historis. Source saat ini sudah memuat implementasi fitur-fitur tersebut; anggota dapat memakai pembagian ini untuk review, perbaikan, dan presentasi.

| Anggota | Fitur | Tanggung jawab dan file utama |
|---|---|---|
| Natasha Aurellia | F01, F09-core | Player, kontrol/command, transisi game. `src/main/java/nangor/core/Player.java`, `Direction.java`, `GameCommand.java`, `GameState.java`, `src/main/java/nangor/runtime/KeyBindings.java`; koordinasikan perubahan `GameEngine.java`. |
| Banu Agil Triyanto | F02, F03, F04, F06 | Bomb, explosion, block, drop/efek power-up. `Bomb.java`, `NormalBomb.java`, `PumpedBomb.java`, `RemoteBomb.java`, `BombType.java`, `Explosion.java`, `Block.java`, `PowerUpType.java`; koordinasikan bagian `Player.java` dan `GameEngine.java`. |
| Muhammad Rizqi Azhar | F00, F05, F07 | Inisialisasi/validasi map, Normal Enemy, Draco, Map A. `MapDefinition.java`, `MapValidationException.java`, `Enemy.java`, `NormalEnemy.java`, `Draco.java`, `MovementStrategy.java`, `PatrolStrategy.java`, `ChaseStrategy.java`, `config/green-garden.map`, `config/green-garden.properties`. |
| Herlambang Setiaji Prabowo | F08, F09-view, integrasi | Score/HUD, terminal/menu, integrasi, tes alur dan dokumentasi. `GameSnapshot.java`, `src/main/java/nangor/terminal/`, `src/main/java/nangor/Main.java`, `src/test/java/nangor/core/`, `README.md`, `doc/`. |

F09 dibagi menurut batas kerja: Natasha memiliki transisi dan command; Herlambang memiliki tampilan layar dan uji alur. `GameEngine.java` merupakan file bersama dan sebaiknya diintegrasikan oleh satu orang setelah perubahan kecil di-review. Pemilik fitur menambah tes aturan; Herlambang menjaga tes integrasi dan renderer.

## Urutan workflow dan push

1. Herlambang memastikan repo GitHub resmi dan akses anggota. Checkout saat ini sudah melacak `origin/main`; verifikasi bahwa `origin` adalah repo resmi sebelum membuat salinan. Jika bukan, clone repo resmi sebagai direktori saudara seperti `/Users/banu/Documents/NangorBlast-M1`, jangan di dalam `TubesOOP`.
2. Jika repo baru diperlukan, Herlambang menyiapkan baseline root berisi `src/`, `config/`, `doc/`, `Makefile`, `README.md`, dan `.gitignore`, kemudian melakukan push pertama ke `main`. Jangan ikutkan `build/`, `target/`, `tmp/`, `.swp`, atau dokumen Milestone 2 yang tidak diminta.
3. Semua anggota membuat branch dari `main`. Sepakati API untuk `GameEngine`, `Player`, `GameCommand`, dan `GameSnapshot` sebelum mengedit file bersama.
4. Urutan kerja/merge yang disarankan: Rizqi F00/F07 (map dan validator); Natasha F01/F09-core; Banu F02/F03/F04/F06; Rizqi F05 (enemy dan boss); Herlambang F08/F09-view, tes integrasi, dan dokumentasi. Pekerjaan bisa berlangsung paralel setelah API disepakati.
5. Setiap pemilik menambah tes fitur, menjalankan `make build` dan `make test`, membuat commit terfokus, lalu push branch. Ajukan PR untuk review; hindari push langsung ke `main` setelah baseline. Herlambang mengintegrasikan PR dan menangani konflik.
6. Sebelum submit M1, cek isi root repo dan render kedua PDF. `milestone-01/` hanya berisi `pembagian-tugas.pdf` dan `struktur-objek.pdf`; folder itu bukan pengganti root repository.

Kode pada checkout ini telah memiliki implementasi F00-F09 menurut source dan README. Pembagian ini menetapkan penanggung jawab review/perbaikan berikutnya, bukan riwayat siapa yang menulis fitur sebelumnya.
