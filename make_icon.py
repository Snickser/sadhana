#!/usr/bin/env python3
"""Иконка приложения: шафрановый квадрат со скруглением + белая мала (16 бусин)."""
import os
import math
from PIL import Image, ImageDraw

BASE = os.path.dirname(os.path.abspath(__file__))
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

SAFFRON = (230, 81, 0, 255)
SAFFRON_DARK = (191, 54, 12, 255)
WHITE = (255, 255, 255, 255)


def make(size):
    scale = 8  # рисуем крупно и уменьшаем для сглаживания
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # фон: скруглённый квадрат с вертикальным градиентом
    radius = int(s * 0.22)
    grad = Image.new("RGBA", (s, s))
    gd = ImageDraw.Draw(grad)
    for y in range(s):
        t = y / max(1, s - 1)
        col = tuple(int(SAFFRON[i] + (SAFFRON_DARK[i] - SAFFRON[i]) * t) for i in range(3)) + (255,)
        gd.line([(0, y), (s, y)], fill=col)
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, s - 1, s - 1], radius=radius, fill=255)
    img.paste(grad, (0, 0), mask)

    d = ImageDraw.Draw(img)
    cx = cy = s / 2.0
    ring_r = s * 0.29
    bead_r = s * 0.055

    # нить
    d.ellipse([cx - ring_r, cy - ring_r, cx + ring_r, cy + ring_r],
              outline=(255, 236, 210, 210), width=max(2, int(s * 0.012)))

    # 16 бусин по кругу
    for k in range(16):
        a = -math.pi / 2 + k * (2 * math.pi / 16)
        bx = cx + ring_r * math.cos(a)
        by = cy + ring_r * math.sin(a)
        r = bead_r * (1.45 if k == 0 else 1.0)
        d.ellipse([bx - r, by - r, bx + r, by + r], fill=WHITE)

    # бусина-гуру в центре и кисточка вниз
    gr = s * 0.075
    d.ellipse([cx - gr, cy - gr, cx + gr, cy + gr], fill=WHITE)
    d.line([(cx, cy + ring_r + bead_r), (cx, cy + ring_r + bead_r + s * 0.06)],
           fill=WHITE, width=max(2, int(s * 0.018)))

    return img.resize((size, size), Image.LANCZOS)


def main():
    for dens, size in SIZES.items():
        out_dir = os.path.join(BASE, "res", "mipmap-" + dens)
        os.makedirs(out_dir, exist_ok=True)
        img = make(size)
        path = os.path.join(out_dir, "ic_launcher.png")
        img.save(path, "PNG")
        print(dens, size, path)


if __name__ == "__main__":
    main()
