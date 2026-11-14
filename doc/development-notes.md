# Catatan pekerjaan sesi implementasi

Catatan ini mendeskripsikan tindakan yang benar-benar dilakukan pada sesi bantuan implementasi. Ini **bukan log kontribusi anggota kelompok**.

1. Folder awal diperiksa: hanya PDF spesifikasi dan file metadata sistem; tidak ada source atau `.git`.
2. Seluruh 61 halaman dibaca melalui ekstraksi teks. Layout map A halaman 39, diagram alur halaman 48, dan halaman Extras diperiksa secara visual.
3. Dibuat konfigurasi map asli, engine satu pemilik state, input queue, clock monotonic, renderer CLI, dan fitur F00-F09 untuk Map A. Tema awal Green Garden kemudian direskin sebagai Stasiun Nangor tanpa mengubah layout atau aturan map.
4. Build lint menemukan subclass Bomb pada file yang sama menimbulkan peringatan auxiliary class. Subclass dipindahkan ke file masing-masing.
5. Suite aturan dikembangkan. Ekspektasi satu jalur BFS diperbaiki setelah pemeriksaan tembok map menunjukkan rute yang lebih pendek.
6. Runner campaign disesuaikan dengan perilaku chase yang benar-benar menghindari penghalang. Strategi trap dan penghindaran warning menghasilkan WIN pada map asli tanpa modifikasi state.
7. Review memperbaiki EOF yang dapat terbuang saat transisi layar, memastikan Swoop hanya memberi damage pada lintasan yang benar-benar ditempuh, dan mencegah normalisasi huruf mengubah kode internal tombol panah.
8. Uji integrasi dan PTY dijalankan. Izin lingkungan diperlukan untuk ioctl terminal; setelah diizinkan, kontrol real-time dan pemulihan terminal lulus.
9. Disiapkan dokumentasi, diagram, buklet A5, pembagian tugas usulan, dan log activity dengan identitas/riwayat kelompok sebagai placeholder.

Rancangan awal kelompok, perubahan rancangan kelompok, rapat, ideasi, asistensi, tanggal kerja tiap anggota, dan bukti foto/screenshot: **[BELUM DIBERIKAN]**. Isi dengan catatan faktual anggota sebelum pengumpulan. Tidak ada commit atau release yang dibuat.
