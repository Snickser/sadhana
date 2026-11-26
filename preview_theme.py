#!/usr/bin/env python3
"""Макет главного экрана в вайшнавском стиле — для проверки композиции глазами.
Повторяет layout activity_main.xml: шапка-градиент, лотос, 4 круглые кнопки,
полоса сводки, карточки отчётов, нижняя панель с «пилюлями».
Это НЕ рендер Android, а приближение: цвета и пропорции из ресурсов."""
import math
from PIL import Image, ImageDraw, ImageFont

D = 3.0  # xxhdpi


def dp(v):
    return v * D


def font(sp, bold=False, serif=False):
    name = "DejaVuSerif" if serif else "DejaVuSans"
    path = "/usr/share/fonts/truetype/dejavu/%s%s.ttf" % (name, "-Bold" if bold else "")
    try:
        return ImageFont.truetype(path, int(sp * D))
    except OSError:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", int(sp * D))


KESAR, KESAR_DEEP, KESAR_LIGHT = (193, 68, 14), (140, 46, 8), (232, 118, 58)
GOLD, GOLD_LIGHT, GOLD_PALE = (200, 161, 90), (232, 211, 169), (247, 233, 206)
CREAM, CARD = (253, 246, 230), (255, 253, 247)
MAROON, INK, INK_SOFT = (107, 31, 31), (59, 36, 22), (138, 106, 82)
WHITE = (255, 255, 255)


def vgrad(size, top, bottom):
    w, h = size
    img = Image.new("RGB", (1, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.point((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img.resize((w, h))


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def soft_circle(img, box, fill_rgba, outline):
    """Полупрозрачный круг с правильной альфой (ImageDraw на RGB альфу игнорирует)."""
    w, h = box[2] - box[0], box[3] - box[1]
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse([0, 0, w - 1, h - 1], fill=fill_rgba)
    base = img.convert("RGBA")
    base.alpha_composite(layer, (box[0], box[1]))
    img.paste(base.convert("RGB"), (0, 0))
    ImageDraw.Draw(img).ellipse(box, outline=outline, width=max(1, int(dp(1))))


def pill(img, box, top, bottom, outline=None, radius=None):
    w, h = box[2] - box[0], box[3] - box[1]
    # радиус не больше половины высоты, иначе низ «подрезается»
    r = radius if radius is not None else min(h // 2, int(dp(23)))
    g = vgrad((w, h), top, bottom)
    img.paste(g, (box[0], box[1]), rounded_mask((w, h), r))
    if outline:
        ImageDraw.Draw(img).rounded_rectangle(box, radius=r, outline=outline, width=max(1, int(dp(1))))


ICON_CACHE = {}


def vector_icon(img, name, cx, cy, size, tint=None):
    """Вставляет настоящий рендер vector drawable (render_vector.py)."""
    from render_vector import render
    key = name
    if key not in ICON_CACHE:
        out = "/opt/data/cache/scratch/_ic_%s.png" % name
        render("res/drawable/%s.xml" % name, out, size_px=192, bg=(0, 0, 0, 0))
        ICON_CACHE[key] = Image.open(out).convert("RGBA")
    ic = ICON_CACHE[key]
    if tint:
        px = ic.load()
        for yy in range(ic.height):
            for xx in range(ic.width):
                r, g, b, a = px[xx, yy]
                if a > 0:
                    px[xx, yy] = tint + (a,)
    ic = ic.resize((int(size), int(size)), Image.LANCZOS)
    img.paste(ic, (int(cx - size / 2), int(cy - size / 2)), ic)


LOTUS_IMG = None


def lotus(img, d, cx, cy, size):
    """Вставляет настоящий рендер ic_lotus.xml (см. render_vector.py)."""
    global LOTUS_IMG
    if LOTUS_IMG is None:
        from render_vector import render
        render("res/drawable/ic_lotus.xml", "/opt/data/cache/scratch/_lotus_tmp.png",
               size_px=192, bg=(193, 68, 14, 255))
        LOTUS_IMG = Image.open("/opt/data/cache/scratch/_lotus_tmp.png").convert("RGBA")
    ic = LOTUS_IMG.resize((int(size), int(size)), Image.LANCZOS)
    img.paste(ic, (int(cx - size / 2), int(cy - size / 2)), ic)


def main():
    W = int(dp(393))
    H = int(dp(800))
    img = Image.new("RGB", (W, H), CREAM)
    d = ImageDraw.Draw(img)

    # ---- шапка ----
    hh = int(dp(56))  # без подзаголовка шапка ниже
    header = vgrad((W, hh), (217, 97, 27), KESAR_DEEP)
    img.paste(header, (0, 0))          # без скруглений
    d = ImageDraw.Draw(img)
    d.rectangle([0, hh - int(dp(2)), W, hh], fill=GOLD)

    lotus(img, d, int(dp(26)), int(dp(28)), int(dp(22)))
    d = ImageDraw.Draw(img)
    d.text((dp(44), dp(19)), "Садхана", font=font(16, bold=True), fill=(255, 248, 231))

    # круглые кнопки с настоящими векторными иконками
    icons = ["ic_stats", "ic_alarm", "ic_heart", "ic_trash"]
    for i, icon in enumerate(icons):
        cx = W - int(dp(8)) - int(dp(38)) * (4 - i) - int(dp(4)) * (4 - i) + int(dp(19))
        cy = int(dp(28))
        r = int(dp(19))
        soft_circle(img, [cx - r, cy - r, cx + r, cy + r], (0, 0, 0, 51), GOLD)
        vector_icon(img, icon, cx, cy, int(dp(20)))
        d = ImageDraw.Draw(img)

    # ---- сводка ----
    y = hh
    sy = y + int(dp(9))
    d.rectangle([0, y, W, sy + int(dp(34))], fill=GOLD_PALE)
    d.line([(0, sy + int(dp(34))), (W, sy + int(dp(34)))], fill=GOLD_LIGHT, width=max(1, int(dp(1))))
    d.text((dp(16), sy), "Всего отчётов: 12 · за 7 дней: 6 · среднее кругов: 16",
           font=font(11), fill=INK)
    d.text((dp(16), sy + dp(16)), "подъём до 5:00: 4 дн.", font=font(11), fill=INK)

    # ---- карточки ----
    y = sy + int(dp(34)) + int(dp(8))
    rows = [
        ("Сегодня, 26 ноября 2026, четверг", "балл 6/7 · чтение 30 мин · служение", "пункты 5/5 · 5:00 · арати · лекция · прасад · 22:00", "16\nкруг."),
        ("25 ноября 2026, среда", "балл 5/7 · чтение 20 мин", "пункты 4/5 · 5:00 · арати · прасад · 22:00", "12\nкруг."),
        ("24 ноября 2026, вторник", "балл 3/7 · чтения нет", "пункты 3/5 · 5:00 · прасад · 22:00", "8\nкруг."),
    ]
    for idx, (date, det, marks, badge) in enumerate(rows):
        ch = int(dp(72))
        box = [dp(10), y, W - dp(10), y + ch]
        img.paste(vgrad((int(box[2] - box[0]), ch), CARD, CARD), (int(box[0]), int(box[1])),
                  rounded_mask((int(box[2] - box[0]), ch), int(dp(14))))
        d.rounded_rectangle(box, radius=int(dp(14)), outline=GOLD if idx == 0 else GOLD_LIGHT,
                            width=max(1, int(dp(1))))
        if idx == 0:  # сегодня: шафрановая полоса слева
            d.rounded_rectangle([box[0], box[1], box[0] + dp(4), box[3]], radius=int(dp(2)), fill=KESAR)
        d.text((dp(26), y + dp(9)), date, font=font(14, bold=True, serif=True), fill=INK)
        d.text((dp(26), y + dp(30)), det, font=font(11), fill=INK_SOFT)
        d.text((dp(26), y + dp(46)), marks, font=font(10), fill=MAROON)
        bw, bh = int(dp(58)), int(dp(44))
        bx, by = int(box[2] - dp(12) - bw), int(y + (ch - bh) / 2)
        pill(img, (bx, by, bx + bw, by + bh), GOLD_PALE, GOLD_LIGHT, outline=GOLD)
        d = ImageDraw.Draw(img)
        for i, line in enumerate(badge.split("\n")):
            f = font(13, bold=True) if i == 0 else font(9)
            tw = d.textlength(line, font=f)
            d.text((bx + (bw - tw) / 2, by + dp(6) + i * dp(17)), line, font=f, fill=MAROON)
        y += ch + int(dp(10))

    # ---- нижняя панель ----
    by = H - int(dp(76))
    d.rectangle([0, by, W, H], fill=CREAM)
    d.line([(0, by), (W, by)], fill=GOLD_LIGHT, width=max(1, int(dp(1))))
    pad = int(dp(10))
    bh = int(dp(46))
    ty = by + int(dp(8))
    total = W - 2 * pad - int(dp(16))
    w1 = int(total / 3.2)
    w3 = int(total * 1.2 / 3.2)
    w2 = total - w1 - w3
    x = pad
    for w, label, primary in ((w1, "Экспорт", False), (w2, "Импорт", False), (w3, "＋ Отчёт", True)):
        if primary:
            pill(img, (x, ty, x + w, ty + bh), (217, 97, 27), (169, 54, 9), outline=GOLD)
            f = font(13, bold=True); col = WHITE
        else:
            pill(img, (x, ty, x + w, ty + bh), CARD, GOLD_PALE, outline=GOLD)
            f = font(12); col = MAROON
        d = ImageDraw.Draw(img)
        tw = d.textlength(label, font=f)
        if primary:
            icon_w = dp(15)
            total_w = icon_w + dp(5) + tw
            ix = x + (w - total_w) / 2
            vector_icon(img, "ic_plus", ix + icon_w / 2, ty + bh / 2, icon_w)
            d = ImageDraw.Draw(img)
            d.text((ix + icon_w + dp(5), ty + bh / 2 - dp(6)), label, font=f, fill=col)
        else:
            d.text((x + (w - tw) / 2, ty + bh / 2 - dp(6)), label, font=f, fill=col)
        x += w + int(dp(8))

    img.save("/opt/data/cache/scratch/vaishnava_main.png")
    print("сохранено: /opt/data/cache/scratch/vaishnava_main.png", img.size)


if __name__ == "__main__":
    main()
