#!/usr/bin/env python3
"""Generates the mod's original 16x16 pixel-art textures and icon. Run from the project root."""
import os, random
from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/voidoutlaws/textures"
os.makedirs(OUT + "/block", exist_ok=True)
os.makedirs(OUT + "/item", exist_ok=True)

def C(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)

def base(col, noise=0.08, seed=1):
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            im.putpixel((x, y), shade(col, 1 + r.uniform(-noise, noise)))
    return im

def frame(im, col, w=1):
    d = ImageDraw.Draw(im)
    for i in range(w):
        d.rectangle([i, i, 15 - i, 15 - i], outline=col)
    return im

def rect(im, x1, y1, x2, y2, col, fill=True):
    d = ImageDraw.Draw(im)
    if fill: d.rectangle([x1, y1, x2, y2], fill=col)
    else: d.rectangle([x1, y1, x2, y2], outline=col)

def px(im, pts, col):
    for (x, y) in pts:
        if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), col)

def line(im, a, b, col):
    ImageDraw.Draw(im).line([a, b], fill=col)

def save(im, kind, name):
    im.save(f"{OUT}/{kind}/{name}.png")

METAL = C('4a4f57'); DARK = C('2b2e33'); LIGHT = C('8a9099')

# ---------------------------------------------------------------- blocks
im = base(METAL, seed=2); frame(im, DARK, 2); rect(im, 3, 3, 12, 9, C('0f2a33'))
for x in range(4, 12, 2): px(im, [(x, 5), (x + 1, 7)], C('39d0e6'))
px(im, [(6, 4), (9, 6), (5, 8), (10, 8)], C('b8f6ff')); rect(im, 3, 11, 5, 12, C('e63939')); rect(im, 7, 11, 8, 12, C('e6c339')); rect(im, 10, 11, 12, 12, C('39e66a'))
save(im, "block", "ship_console")

im = base(METAL, seed=3); frame(im, DARK, 2); rect(im, 3, 3, 12, 12, C('1d1416'))
line(im, (7, 3), (7, 12), C('ff3b3b')); line(im, (8, 3), (8, 12), C('ff3b3b')); line(im, (3, 7), (12, 7), C('ff3b3b')); line(im, (3, 8), (12, 8), C('ff3b3b'))
rect(im, 5, 5, 10, 10, C('ff7a7a'), fill=False); px(im, [(7, 7), (8, 8), (7, 8), (8, 7)], C('ffffff'))
save(im, "block", "gunnery_station")

im = base(METAL, seed=4); frame(im, DARK, 1)
for i, cx in enumerate([4, 11]):
    rect(im, cx - 2, 2, cx + 2, 6, C('101810')); px(im, [(cx, 4), (cx + 1, 3)], C('5cff5c')); px(im, [(cx - 1, 5)], C('5cff5c'))
for y in (9, 11, 13): line(im, (2, y), (13, y), C('c47a2c') if y == 11 else C('2c7ac4'))
px(im, [(13, 9), (13, 11), (13, 13)], C('ffe066'))
save(im, "block", "engineering_panel")

im = base(C('9c7a46'), seed=5); frame(im, C('5c4426'), 1)
for y in (5, 10): line(im, (1, y), (14, y), C('5c4426'))
for x in range(1, 15):
    px(im, [(x, 0), (x, 15)], C('e6c229') if (x // 2) % 2 == 0 else C('1a1a1a'))
save(im, "block", "cargo_hold")

im = base(C('3d3d45'), noise=0.04, seed=6)
for y in (0, 8): line(im, (0, y), (15, y), C('2e2e35'))
for x in (0, 8): line(im, (x, 0), (x, 15), C('2e2e35'))
px(im, [(3, 3), (12, 3), (3, 12), (12, 12)], C('5a5a66')); line(im, (5, 13), (10, 13), C('34343b'))
save(im, "block", "hidden_compartment")

im = Image.new("RGBA", (16, 16))
for x in range(16):
    for y in range(16):
        im.putpixel((x, y), C('e6c229') if ((x + y) // 3) % 2 == 0 else C('1f1f1f'))
rect(im, 3, 3, 12, 12, METAL); rect(im, 4, 7, 11, 8, C('20242a')); px(im, [(4, 4), (11, 4), (4, 11), (11, 11)], LIGHT)
save(im, "block", "ship_hatch")

im = base(METAL, seed=7); frame(im, DARK, 1)
for r, col in [(6, C('2c6c8c')), (4, C('3fb7e6')), (2, C('bff3ff'))]:
    rect(im, 8 - r, 8 - r, 7 + r, 7 + r, col)
px(im, [(7, 7), (8, 8), (7, 8), (8, 7)], C('ffffff'))
save(im, "block", "docking_beacon")

im = base(C('a87443'), noise=0.15, seed=8); frame(im, C('5a3a1e'), 1)
for (x, y, w, h, col) in [(2, 2, 5, 5, C('f2efe4')), (9, 3, 5, 4, C('f7e08a')), (3, 9, 4, 5, C('cfe8ff')), (9, 9, 5, 5, C('f2efe4'))]:
    rect(im, x, y, x + w - 1, y + h - 1, col); px(im, [(x + w // 2, y)], C('d23b3b'))
    for yy in range(y + 2, y + h - 1, 2): line(im, (x + 1, yy), (x + w - 2, yy), C('8a8a8a'))
save(im, "block", "job_board")

im = base(C('3d6b3d'), seed=9); frame(im, C('243f24'), 1); rect(im, 2, 2, 13, 4, C('2a4a2a')); line(im, (5, 3), (10, 3), C('121212'))
for x in (4, 8, 12): line(im, (x, 6), (x, 14), C('2f552f'))
save(im, "block", "dead_drop")

im = base(C('1b1024'), seed=10); frame(im, C('7a2ce6'), 1); rect(im, 3, 3, 12, 12, C('2a1038'))
for (x, y) in [(7, 4), (8, 4), (6, 5), (6, 6), (7, 7), (8, 7), (9, 8), (9, 9), (7, 10), (8, 10), (7, 3), (7, 11), (8, 3), (8, 11)]:
    px(im, [(x, y)], C('e04cff'))
save(im, "block", "black_market")

im = base(C('1f6b3a'), seed=11); frame(im, C('6b3d1f'), 2)
rect(im, 3, 4, 6, 9, C('f5f5f5')); px(im, [(4, 5), (5, 8)], C('d23b3b'))
rect(im, 9, 6, 12, 11, C('f5f5f5')); px(im, [(10, 7), (11, 10)], C('222222'))
save(im, "block", "card_table")

im = base(C('3a3a3a'), seed=12); frame(im, C('d4af37'), 2); rect(im, 3, 3, 12, 6, C('0a1a0a')); line(im, (4, 4), (10, 4), C('39e66a'))
for i in range(9):
    x, y = 4 + (i % 3) * 3, 8 + (i // 3) * 2
    px(im, [(x, y), (x + 1, y)], C('e63939') if i == 4 else C('bbbbbb'))
save(im, "block", "vault_terminal")

im = base(C('6b7078'), seed=13); frame(im, C('3a3d42'), 1)
for x in (3, 12): line(im, (x, 1), (x, 14), C('d4af37'))
line(im, (1, 7), (14, 7), C('d4af37')); rect(im, 7, 6, 8, 8, C('fff1a8'))
save(im, "block", "loot_crate")

im = base(C('c9a227'), seed=14); frame(im, C('7a5f12'), 1); line(im, (7, 1), (7, 14), C('7a5f12'))
rect(im, 9, 6, 11, 9, C('5a4a1a')); px(im, [(10, 7)], C('f0e0a0'))
for y in (3, 4, 11, 12): line(im, (2, y), (5, y), C('8a6e18'))
save(im, "block", "loot_locker")

im = base(C('e8eef2'), noise=0.03, seed=15); frame(im, C('6fa8d6'), 1); rect(im, 3, 2, 12, 8, C('1d3c55')); line(im, (4, 4), (11, 4), C('8fd3ff')); line(im, (4, 6), (9, 6), C('8fd3ff'))
rect(im, 6, 10, 9, 13, C('6fa8d6')); px(im, [(7, 11), (8, 12)], C('ffffff'))
save(im, "block", "fine_kiosk")

im = base(C('4f9a83'), noise=0.15, seed=16); frame(im, C('2f5f50'), 1); rect(im, 3, 3, 12, 12, C('243a33'))
for i, col in enumerate([C('39e66a'), C('e6c339'), C('e63939')]):
    rect(im, 4 + i * 3, 4, 5 + i * 3, 5, col)
rect(im, 7, 7, 8, 12, LIGHT); rect(im, 6, 7, 9, 8, C('d23b3b'))
save(im, "block", "bulkhead_panel")

# ---------------------------------------------------------------- items
def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

im = blank(); r = random.Random(20)
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, (y - 11) * 1.7
        if dx * dx + dy * dy < 40 and y > 5: im.putpixel((x, y), shade(C('5b2a8c'), r.uniform(0.8, 1.3)))
px(im, [(5, 8), (9, 7), (7, 10), (11, 11), (4, 12)], C('f0d8ff')); px(im, [(8, 4), (3, 6), (12, 5)], C('d8a8ff'))
save(im, "item", "void_dust")

im = blank(); r = random.Random(21)
for x in range(2, 14):
    for y in range(4, 14):
        dx, dy = x - 7.5, y - 9
        if dx * dx + dy * dy * 1.3 < 30 + r.uniform(-6, 6): im.putpixel((x, y), shade(C('3fbf4a'), r.uniform(0.7, 1.3)))
px(im, [(6, 7), (9, 9), (5, 11), (10, 6), (8, 12)], C('c8ffb0'))
save(im, "item", "glowmoss")

im = blank(); rect(im, 3, 3, 12, 12, C('1d5c2e')); rect(im, 5, 5, 10, 10, C('222222'))
for i in range(4, 12, 2): px(im, [(i, 2), (i, 13), (2, i), (13, i)], C('d4af37'))
line(im, (5, 7), (10, 7), C('39e66a')); px(im, [(6, 9), (9, 6)], C('e63939'))
save(im, "item", "stolen_tech")

im = blank()
for (x1, y1, x2, y2, col) in [(6, 2, 9, 13, C('6fe6f0')), (4, 6, 5, 13, C('3fb0c0')), (10, 5, 11, 13, C('3fb0c0'))]:
    rect(im, x1, y1, x2, y2, col)
px(im, [(7, 3), (7, 4), (6, 6)], C('e8ffff')); line(im, (3, 14), (12, 14), C('556677'))
save(im, "item", "hum_crystal")

im = blank()
for x in range(16):
    for y in range(16):
        dx, dy = (x - 7.5) / 5.2, (y - 8.5) / 6.5
        if dx * dx + dy * dy < 1: im.putpixel((x, y), C('e8a04c'))
px(im, [(5, 5), (9, 4), (10, 9), (6, 11), (8, 8), (4, 9), (11, 12)], C('8a4a1a')); px(im, [(6, 4), (5, 6)], C('ffd7a0'))
save(im, "item", "grumbleback_egg")

im = blank(); rect(im, 2, 6, 12, 8, C('707880')); rect(im, 4, 9, 6, 13, C('4a3020')); rect(im, 12, 6, 14, 7, C('e63939'))
px(im, [(8, 9), (8, 10)], C('707880')); line(im, (3, 5), (9, 5), C('a0a8b0')); px(im, [(5, 7)], C('39d0e6'))
save(im, "item", "blaster")

im = blank(); rect(im, 3, 2, 6, 11, C('8a9099')); rect(im, 9, 2, 12, 11, C('8a9099')); rect(im, 6, 4, 9, 7, C('4a4f57'))
px(im, [(4, 3), (10, 3)], C('e6c229'))
for x in (4, 5, 10, 11): px(im, [(x, 12), (x, 13)], C('ff8c1a')); px(im, [(x, 14)], C('ffe066'))
save(im, "item", "jetpack")

im = blank(); line(im, (3, 13), (9, 7), C('c8b48a')); line(im, (4, 13), (10, 7), C('a08c60'))
for (x, y) in [(10, 6), (11, 5), (12, 4), (12, 3), (11, 2), (13, 5), (13, 7), (12, 8), (9, 3), (8, 4)]: px(im, [(x, y)], C('a0a8b0'))
save(im, "item", "grappling_hook")

im = blank(); rect(im, 2, 4, 6, 7, C('222222'), fill=False); rect(im, 9, 4, 13, 7, C('222222'), fill=False); line(im, (6, 5), (9, 5), C('222222'))
px(im, [(3, 5), (10, 5)], C('9fd8ff'))
for (x, y) in [(3, 11), (4, 10), (5, 10), (6, 10), (7, 11), (8, 11), (9, 10), (10, 10), (11, 10), (12, 11), (2, 12), (13, 12)]: px(im, [(x, y)], C('5a3a1e'))
save(im, "item", "disguise_kit")

im = blank(); rect(im, 4, 4, 11, 11, C('1d6b2e')); rect(im, 6, 6, 9, 9, C('d4af37'))
for i in (5, 7, 9): px(im, [(i, 3), (i, 12)], C('a0a8b0'))
save(im, "item", "transponder_chip")

im = blank(); rect(im, 3, 3, 12, 12, C('8a9099')); rect(im, 3, 3, 12, 12, C('5a6068'), fill=False)
px(im, [(4, 4), (11, 4), (4, 11), (11, 11)], C('d4d8dc')); line(im, (5, 8), (10, 8), C('a0a8b0'))
save(im, "item", "hull_patch")

im = blank(); rect(im, 3, 7, 10, 10, C('ff7ab8')); rect(im, 10, 8, 13, 9, C('ff9fd0')); rect(im, 5, 6, 6, 6, C('ffd0e8'))
px(im, [(5, 8)], C('7a2a52')); line(im, (2, 11), (2, 13), C('d4af37'))
save(im, "item", "companion_whistle")

im = blank()
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, y - 8.5
        if dx * dx + dy * dy < 26: im.putpixel((x, y), C('e63939'))
px(im, [(5, 6), (6, 5)], C('ff9a9a')); rect(im, 6, 2, 9, 3, C('3fbf4a')); px(im, [(7, 1), (5, 3), (10, 3)], C('2f8f3a'))
save(im, "item", "space_tomato")

im = blank(); rect(im, 3, 2, 12, 13, C('7a4a24')); rect(im, 4, 3, 11, 12, C('9c6232')); line(im, (3, 2), (3, 13), C('4a2a12'))
for (x, y) in [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (6, 8), (9, 8)]: px(im, [(x, y)], C('ffe066'))
save(im, "item", "handbook")

im = blank(); rect(im, 3, 2, 12, 13, C('1a1a1a')); rect(im, 3, 2, 12, 13, C('d4af37'), fill=False); rect(im, 6, 6, 9, 9, C('b04cff'))
save(im, "item", "lumen_ledger")

im = blank(); rect(im, 2, 9, 13, 10, C('d8d0c0')); rect(im, 3, 11, 12, 12, C('c0b8a8')); rect(im, 5, 13, 10, 13, C('a8a090'))
for x in range(3, 13): px(im, [(x, 8 if x % 2 else 7)], C('f0d070'))
line(im, (11, 2), (8, 8), C('8a5a2a')); line(im, (13, 3), (9, 8), C('8a5a2a'))
save(im, "item", "nebula_noodles")

# ---------------------------------------------------------------- icon (128x128)
ic = Image.new("RGBA", (128, 128), C('0b0f1e'))
r = random.Random(99)
d = ImageDraw.Draw(ic)
for _ in range(90):
    x, y = r.randrange(128), r.randrange(128)
    d.point((x, y), fill=(255, 255, 255, r.randint(90, 255)))
d.polygon([(20, 70), (64, 40), (108, 70), (64, 82)], fill=C('8a9099'))
d.polygon([(48, 60), (64, 48), (80, 60), (64, 66)], fill=C('39d0e6'))
d.rectangle([40, 80, 52, 92], fill=C('ff8c1a')); d.rectangle([76, 80, 88, 92], fill=C('ff8c1a'))
d.rectangle([14, 100, 114, 116], fill=C('e63939'))
for i, x in enumerate(range(20, 110, 14)):
    d.rectangle([x, 104, x + 8, 112], fill=C('ffe066') if i % 2 == 0 else C('1f1f1f'))
ic.save("src/main/resources/assets/voidoutlaws/icon.png")
print("textures written")
