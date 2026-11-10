"""Genera los loops de PRUEBA del hito 1 (no son los beats finales).

Cada loop tiene un clic agudo en el primer tiempo de cada compás, para poder oír
si el empalme del loop tiene corte y si la voz queda alineada al mezclar.

Uso:  python tool/gen_test_loops.py   (necesita numpy, scipy y ffmpeg)
"""
import json, os, subprocess
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "assets", "beats")
rng = np.random.default_rng(7)


def env(n, decay):
    return np.exp(-np.arange(n) / (SR * decay))


def kick():
    n = int(SR * 0.35)
    t = np.arange(n) / SR
    freq = 45 + 110 * np.exp(-t * 28)
    body = np.sin(2 * np.pi * np.cumsum(freq) / SR) * env(n, 0.11)
    return body + 0.25 * np.tanh(body * 4)  # armónicos, para que se oiga en parlante chico


def snare():
    n = int(SR * 0.25)
    t = np.arange(n) / SR
    b, a = butter(2, [900 / (SR / 2), 7000 / (SR / 2)], "band")
    noise = lfilter(b, a, rng.standard_normal(n)) * env(n, 0.07)
    return 0.9 * noise + 0.5 * np.sin(2 * np.pi * 190 * t) * env(n, 0.04)


def hat():
    n = int(SR * 0.06)
    b, a = butter(2, 7000 / (SR / 2), "high")
    return lfilter(b, a, rng.standard_normal(n)) * env(n, 0.012)


def click():
    n = int(SR * 0.03)
    t = np.arange(n) / SR
    return np.sin(2 * np.pi * 1800 * t) * env(n, 0.006)


def bass(freq, dur):
    n = int(SR * dur)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * freq * t) + 0.5 * np.sin(2 * np.pi * 2 * freq * t) + 0.2 * np.sin(2 * np.pi * 3 * freq * t)
    fade = np.minimum(1, np.minimum(np.arange(n), n - np.arange(n)) / (SR * 0.01))
    return s * fade


def render(bpm, bars, pattern):
    beat = 60 / bpm
    n = int(round(bars * 4 * beat * SR))
    # Se arma el loop tres veces seguido y se usa la copia del medio: así las colas
    # (el eco de la caja) que pasan del final vuelven a entrar por el principio.
    mix = np.zeros(n * 3 + SR * 2)

    def put(sound, when_beats, gain):
        for rep in range(3):
            i = rep * n + int(round(when_beats * beat * SR))
            mix[i:i + len(sound)] += sound * gain

    k, s, h, c = kick(), snare(), hat(), click()
    echo = np.concatenate([s, np.zeros(int(beat * 0.75 * SR))])
    d = int(beat * 0.75 * SR)
    echo[d:d + len(s)] += s * 0.25
    for bar in range(bars):
        o = bar * 4
        put(c, o, 0.5)
        for x in pattern["kick"]:
            put(k, o + x, 0.95)
        for x in pattern["snare"]:
            put(echo, o + x, 0.6)
        for x in pattern["hat"]:
            put(h, o + x, 0.22 if (x * 2) % 1 else 0.3)
        for x, f, dur in pattern["bass"][bar % len(pattern["bass"])]:
            put(bass(f, dur * beat), o + x, 0.3)
    mid = mix[n:2 * n]
    mid = mid / np.max(np.abs(mid)) * 0.7  # pico en unos -3 dBFS
    return np.stack([mid, mid], axis=1)


LOOPS = [
    dict(id="prueba_boombap_92", name="Prueba boom bap", style="Boom bap", bpm=92, bars=8, pattern=dict(
        kick=[0, 1.5, 2.5], snare=[1, 3],
        hat=[i * 0.5 + (0.08 if i % 2 else 0) for i in range(8)],  # swing
        bass=[[(0, 82.4, 1.0), (1.5, 82.4, 0.5), (2.5, 98.0, 1.0)], [(0, 73.4, 1.0), (1.5, 73.4, 0.5), (2.5, 61.7, 1.2)]])),
    dict(id="prueba_trap_140", name="Prueba trap", style="Trap", bpm=140, bars=16, pattern=dict(
        kick=[0, 2.75], snare=[2],
        hat=[i * 0.25 for i in range(16)],
        bass=[[(0, 55.0, 2.5), (2.75, 55.0, 1.2)], [(0, 65.4, 2.5), (2.75, 49.0, 1.2)]])),
]

manifest = []
for L in LOOPS:
    audio = render(L["bpm"], L["bars"], L["pattern"])
    wav = os.path.join(OUT, L["id"] + ".wav")
    ogg = os.path.join(OUT, L["id"] + ".ogg")
    wavfile.write(wav, SR, (audio * 32767).astype(np.int16))
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "6", ogg], check=True)
    os.remove(wav)
    manifest.append(dict(id=L["id"], name=L["name"], style=L["style"], bpm=L["bpm"], bars=L["bars"],
                         seconds=round(len(audio) / SR, 2), file=L["id"] + ".ogg"))
    print(L["id"], len(audio), "muestras", os.path.getsize(ogg) // 1024, "KB")

with open(os.path.join(OUT, "manifest.json"), "w", encoding="utf-8") as f:
    json.dump(dict(beats=manifest), f, ensure_ascii=False, indent=2)
