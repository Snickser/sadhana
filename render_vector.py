#!/usr/bin/env python3
"""Рендер настоящего vector drawable (ic_lotus.xml) в PNG.

Своего SVG-рендера в системе нет, поэтому разбираю pathData вручную: поддерживаются
M, L, C, Z — этого достаточно для иконок проекта. Кривые Безье разбиваются на отрезки,
заливка — полигонами PIL с 4× суперсэмплингом.
"""
import re
import sys
from PIL import Image, ImageDraw

SS = 4  # суперсэмплинг


def parse_path(d):
    """Возвращает список подпутей: [(points, fill), ...] по порядку fill в XML."""
    tokens = re.findall(r"[MLCZmlcz]|-?\d*\.?\d+", d)
    subpaths, cur, i = [], [], 0
    x = y = 0.0
    start = (0.0, 0.0)
    cmd = None
    while i < len(tokens):
        t = tokens[i]
        if t.isalpha():
            cmd = t
            i += 1
            if cmd in "Zz":
                if cur:
                    cur.append(start)
                    subpaths.append(cur)
                    cur = []
                continue
            if cmd in "Mm" and not cur:
                pass
            continue
        if cmd in "Mm":
            x, y = float(tokens[i]), float(tokens[i + 1]); i += 2
            if cur:
                subpaths.append(cur)
            cur = [(x, y)]
            start = (x, y)
            cmd = "L" if cmd == "M" else "l"
        elif cmd in "Ll":
            x, y = float(tokens[i]), float(tokens[i + 1]); i += 2
            cur.append((x, y))
        elif cmd in "Cc":
            x1, y1, x2, y2, x3, y3 = (float(v) for v in tokens[i:i + 6]); i += 6
            p0 = (x, y)
            for k in range(1, 17):  # 16 отрезков на кривую
                s = k / 16.0
                mt = 1 - s
                bx = (mt ** 3) * p0[0] + 3 * (mt ** 2) * s * x1 + 3 * mt * (s ** 2) * x2 + (s ** 3) * x3
                by = (mt ** 3) * p0[1] + 3 * (mt ** 2) * s * y1 + 3 * mt * (s ** 2) * y2 + (s ** 3) * y3
                cur.append((bx, by))
            x, y = x3, y3
        else:
            i += 1
    if cur:
        subpaths.append(cur)
    return subpaths


def render(xml_path, out_png, size_px=480, bg=(193, 68, 14, 255)):
    src = open(xml_path, encoding="utf-8").read()
    vw = float(re.search(r'android:viewportWidth="([\d.]+)"', src).group(1))
    vh = float(re.search(r'android:viewportHeight="([\d.]+)"', src).group(1))
    paths = re.findall(r'android:fillColor="(#[0-9A-Fa-f]+)"\s+android:pathData="([^"]+)"', src)
    if not paths:
        paths = re.findall(r'android:pathData="([^"]+)"\s+android:fillColor="(#[0-9A-Fa-f]+)"', src)
        paths = [(c, p) for p, c in paths]

    W = H = size_px * SS
    img = Image.new("RGBA", (W, H), bg)
    d = ImageDraw.Draw(img)
    scale = W / vw
    for color, data in paths:
        rgb = tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
        for pts in parse_path(data):
            d.polygon([(px * scale, py * scale) for px, py in pts], fill=rgb + (255,))
    img = img.resize((size_px, size_px), Image.LANCZOS)
    img.save(out_png)
    print("сохранено:", out_png, img.size, "| путей:", len(paths))


if __name__ == "__main__":
    render(sys.argv[1], sys.argv[2])
