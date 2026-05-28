#!/usr/bin/env python3
"""Generate simple game sound effects as .wav files."""

import math
import wave
import struct
import os

SAMPLE_RATE = 44100
AMPLITUDE = 0.45

def write_wav(path, samples):
    with wave.open(path, 'w') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        for s in samples:
            wav.writeframes(struct.pack('<h', int(s * 32767 * AMPLITUDE)))


def correct_sound():
    """Light two-step chime for a successful keypad tap."""
    duration = 0.22
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        env = min(1.0, t / 0.012) * math.exp(-t * 13)
        first = math.sin(2 * math.pi * 740 * t) * 0.45
        second_t = max(0.0, t - 0.055)
        second_env = 0.0 if t < 0.055 else math.exp(-second_t * 16)
        second = math.sin(2 * math.pi * 1110 * t) * 0.38 * second_env
        sparkle = math.sin(2 * math.pi * 2220 * t) * 0.08
        s = first + second + sparkle
        samples.append(s * env)
    return samples


def error_sound():
    """Warm muted knock for an incorrect keypad tap."""
    duration = 0.18
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        env = min(1.0, t / 0.01) * math.exp(-t * 18)
        drop = 190 - 55 * min(1.0, t / duration)
        s = math.sin(2 * math.pi * drop * t) * 0.62 \
          + math.sin(2 * math.pi * 95 * t) * 0.22
        samples.append(s * env)
    return samples


if __name__ == '__main__':
    raw_dir = os.path.join(
        os.path.dirname(__file__),
        'app', 'src', 'main', 'res', 'raw'
    )
    os.makedirs(raw_dir, exist_ok=True)

    write_wav(os.path.join(raw_dir, 'correct.wav'), correct_sound())
    write_wav(os.path.join(raw_dir, 'error.wav'), error_sound())
    print(f"Generated sounds in: {raw_dir}")
