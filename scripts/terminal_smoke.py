#!/usr/bin/env python3
"""Exercise the real jar in a POSIX pseudo-terminal, including stty restoration."""
import errno
import os
import pty
import re
import select
import signal
import struct
import termios
import time
import fcntl
from pathlib import Path

pid, terminal = pty.fork()
if pid == 0:
    os.execvp("java", ["java", "-jar", "build/nangor-blast.jar", "--seed", "17"])

original = termios.tcgetattr(terminal)
fcntl.ioctl(terminal, termios.TIOCSWINSZ, struct.pack("HHHH", 30, 80, 0, 0))
output = bytearray()


def read_for(seconds):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if select.select([terminal], [], [], min(0.05, max(0, deadline - time.monotonic())))[0]:
            try:
                data = os.read(terminal, 65536)
                if not data:
                    break
                output.extend(data)
            except OSError as exc:
                if exc.errno == errno.EIO:
                    break
                raise


def text():
    return re.sub(r"\x1b\[[0-9;?]*[A-Za-z]", "", output.decode(errors="replace"))


def key(value, wait=0.2):
    os.write(terminal, value.encode())
    read_for(wait)


def expect(value):
    assert value in text(), f"Missing terminal output: {value}\n{text()[-2000:]}"


try:
    read_for(0.6)
    expect("Start Game")
    raw = termios.tcgetattr(terminal)
    assert not raw[3] & termios.ICANON and not raw[3] & termios.ECHO, "Terminal is not raw"
    key("x"); expect("Input tidak valid")
    key("2"); expect("Draco rentan")
    key("m"); key("1"); expect("PLAYING"); expect("Nyawa 3/5")
    key("\x1b[C", 0.35); expect("Power-up: REMOTE")
    key("b", 0.1)
    assert re.search(r"Bomb NORMAL\s+1/1", text()), "Bomb capacity not displayed"
    key("a", 0.35); key("s", 0.35)
    key("p"); expect("PAUSED:")
    pause_time = re.findall(r"Waktu (\d+)s", text())[-1]
    checkpoint = len(output)
    read_for(3.8)
    assert len(output) == checkpoint, "Paused display changed as time advanced"
    assert re.findall(r"Waktu (\d+)s", text())[-1] == pause_time
    key("p", 3.1)
    assert re.search(r"Bomb NORMAL\s+0/1", text()), "Bomb capacity not restored"
    key("p"); key("r"); expect("Waktu 300s")
    key("p"); key("m"); key("3")
    for _ in range(30):
        read_for(0.1)
        done, status = os.waitpid(pid, os.WNOHANG)
        if done:
            assert os.waitstatus_to_exitcode(status) == 0, "Game exited with error"
            break
    else:
        raise AssertionError("Game did not exit")
    restored = termios.tcgetattr(terminal)
    assert restored == original, "Original terminal settings were not restored"
    assert "Exception" not in text(), "Unhandled Java exception"
    Path("build/terminal-smoke.txt").write_text(text())
    print("TerminalSmoke: PASS real-time keys, help, invalid input, movement, bomb, pause > fuse, resume, restart, menu, exit, terminal restored")
finally:
    try:
        os.kill(pid, signal.SIGTERM)
    except ProcessLookupError:
        pass
    os.close(terminal)
