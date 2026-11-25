#!/usr/bin/env python3
"""Макет геометрии BarChartView на PIL — проверка, что столбики и подписи не наезжают.
Это НЕ рендер Android: формулы повторяют onDraw из BarChartView.java 1:1,
чтобы глазами оценить пропорции до установки на телефон."""
import math
import random
from PIL import Image, ImageDraw, ImageFont

# xxhdpi: density=3, scaledDensity=3
DENSITY = 3.0


def dp(v):
    return v * DENSITY


def sp(v):
    return v * DENSITY


def font(size_px):
    for path in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                 "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"):
        try:
            return ImageFont.truetype(path, int(size_px))
        except OSError:
            continue
    return ImageFont.load_default()


STEPS = [1, 2, 4, 8, 10, 16, 20, 25, 32, 50, 100, 150, 200, 300, 500, 1000]


def nice_max(value):
    if value <= 1:
        return 1
    for s in STEPS:
        if value <= s:
            return s
    return ((value + 999) // 1000) * 1000


def next_step(current):
    for s in STEPS:
        if s > current:
            return s
    return current + 1000 - (current % 1000)


def nice_max_with_headroom(max_value, target):
    floor = max(max_value, target)
    top = nice_max(floor)
    if top <= max_value:
        top = next_step(top)
    return top


def draw_chart(w, h, values, labels, present, target, title, unit, bar_color):
    img = Image.new("RGB", (int(w), int(h)), (255, 255, 255))
    d = ImageDraw.Draw(img)
    n = len(values)

    f_title = font(sp(13))
    f_axis = font(sp(9))
    d.text((dp(4), dp(4)), title, fill=(62, 39, 35), font=f_title)

    left, right = dp(30), w - dp(4)
    top, bottom = dp(28), h - dp(16)
    plot_w, plot_h = max(1.0, right - left), max(1.0, bottom - top)

    mx = target
    for v in values:
        if v > mx:
            mx = v
    if mx <= 0:
        mx = 1
    mx = nice_max_with_headroom(mx, target)

    for level in (0, mx // 2, mx):
        y = bottom - plot_h * level / mx
        d.line([(left, y), (right, y)], fill=(240, 228, 216), width=max(1, int(dp(1))))
        d.text((dp(2), y - sp(4)), str(level), fill=(141, 110, 99), font=f_axis)

    if 0 < target <= mx:
        y = bottom - plot_h * target / mx
        x = left
        while x < right:  # пунктир
            d.line([(x, y), (min(x + dp(5), right), y)], fill=(176, 133, 91),
                   width=max(1, int(dp(1.4))))
            x += dp(9)

    slot = plot_w / n
    bar_w = max(dp(1.5), slot * 0.62)
    for i in range(n):
        cx = left + slot * i + slot / 2
        v = values[i]
        if v <= 0:
            d.rectangle([cx - bar_w / 2, bottom - dp(1), cx + bar_w / 2, bottom],
                        fill=(232, 221, 211))
            continue
        bh = plot_h * v / mx
        color = bar_color if (i < len(present) and present[i]) else (232, 221, 211)
        d.rectangle([cx - bar_w / 2, bottom - bh, cx + bar_w / 2, bottom], fill=color)

    step = max(1, math.ceil(n / 6))
    for i in range(0, n, step):
        cx = left + slot * i + slot / 2
        s = labels[i] if i < len(labels) else ""
        tw = d.textlength(s, font=f_axis)
        d.text((cx - tw / 2, bottom + sp(2)), s, fill=(141, 110, 99), font=f_axis)

    if unit:
        tw = d.textlength(unit, font=f_axis)
        d.text((right - tw, top - dp(4) - sp(9)), unit, fill=(161, 136, 127), font=f_axis)
    return img


def sample(days):
    random.seed(7)
    labels, values, present = [], [], []
    for i in range(days):
        labels.append(f"{((i * 3) % 28) + 1:02d}.11")
        if random.random() < 0.72:
            present.append(True)
            values.append(random.choice([4, 8, 10, 16, 16, 16, 20, 25, 12]))
        else:
            present.append(False)
            values.append(0)
    return labels, values, present


def main():
    w, h = dp(360), dp(180)
    for days, name in ((7, "7"), (30, "30"), (90, "90")):
        labels, values, present = sample(days)
        chart = draw_chart(w, h, values, labels, present, 16,
                           f"Круги джапы — {days} дней", "цель 16", (230, 81, 0))
        chart.save(f"/opt/data/cache/scratch/chart_preview_{name}.png")
        print("сохранено:", f"/opt/data/cache/scratch/chart_preview_{name}.png", chart.size)

    # три графика вместе, как на экране
    labels, values, present = sample(30)
    charts = [
        draw_chart(w, h, values, labels, present, 16, "Круги джапы", "цель 16", (230, 81, 0)),
        draw_chart(w, h, [min(v // 4, 5) for v in values], labels, present, 5,
                   "Регулярные пункты", "из 5", (109, 140, 78)),
        draw_chart(w, h, [v * 2 for v in values], labels, present, 0,
                   "Чтение, минут", "мин", (126, 87, 194)),
    ]
    total_h = sum(c.height for c in charts) + dp(12) * (len(charts) + 1)
    sheet = Image.new("RGB", (int(w + dp(24)), int(total_h)), (255, 248, 240))
    y = dp(12)
    for c in charts:
        sheet.paste(c, (int(dp(12)), int(y)))
        y += c.height + dp(12)
    sheet.save("/opt/data/cache/scratch/charts_screen.png")
    print("экран целиком:", "/opt/data/cache/scratch/charts_screen.png", sheet.size)


if __name__ == "__main__":
    main()
