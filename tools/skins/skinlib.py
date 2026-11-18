"""Tiny pixel-art toolkit for painting 64x64 Minecraft skins (1.8+ layout, both layers).

Every body part is a box; each face of each box can be painted with a function
fn(x, y, w, h) -> colour | None, or with an explicit character grid + palette.
A final pass adds per-face lighting and subtle material noise so the result reads
as hand-shaded pixel art rather than flat fills."""
import random
from PIL import Image

# (u, v, w, h, d) for each part; layer-2 parts share dimensions with their base.
def parts(slim):
    aw = 3 if slim else 4
    return {
        "head": (0, 0, 8, 8, 8),     "hat": (32, 0, 8, 8, 8),
        "body": (16, 16, 8, 12, 4),  "jacket": (16, 32, 8, 12, 4),
        "rarm": (40, 16, aw, 12, 4), "rsleeve": (40, 32, aw, 12, 4),
        "larm": (32, 48, aw, 12, 4), "lsleeve": (48, 48, aw, 12, 4),
        "rleg": (0, 16, 4, 12, 4),   "rpants": (0, 32, 4, 12, 4),
        "lleg": (16, 48, 4, 12, 4),  "lpants": (0, 48, 4, 12, 4),
    }

FACES = ("top", "bottom", "right", "front", "left", "back")
LIGHT = {"top": 1.10, "bottom": 0.72, "right": 0.88, "front": 1.0, "left": 0.88, "back": 0.80}

def face_rect(u, v, w, h, d, face):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }[face]

def hexc(c, a=255):
    if c is None: return None
    if isinstance(c, tuple): return c if len(c) == 4 else (*c, a)
    c = c.lstrip("#")
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)

def mix(a, b, t):
    a, b = hexc(a), hexc(b)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))

def shade(c, f):
    c = hexc(c)
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])

class Skin:
    def __init__(self, slim, seed=1):
        self.slim = slim
        self.P = parts(slim)
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.mat = {}          # (x, y) -> noise amount
        self.facemap = {}      # (x, y) -> face name (for lighting)
        self.nolight = set()   # pixels that keep their exact colour (eyes, highlights)
        self.rng = random.Random(seed)

    def rect(self, part, face):
        return face_rect(*self.P[part], face)

    def paint(self, part, faces, fn, noise=0.05, light=True):
        if isinstance(faces, str): faces = FACES if faces == "all" else (faces,)
        for face in faces:
            x0, y0, w, h = self.rect(part, face)
            for y in range(h):
                for x in range(w):
                    c = fn(face, x, y, w, h)
                    if c is None: continue
                    self.put(x0 + x, y0 + y, c, noise, face, light)

    def put(self, px, py, c, noise=0.05, face="front", light=True):
        c = hexc(c)
        if c is None: return
        self.img.putpixel((px, py), c)
        self.mat[(px, py)] = noise
        self.facemap[(px, py)] = face
        if not light: self.nolight.add((px, py))
        else: self.nolight.discard((px, py))

    def grid(self, part, face, rows, pal, noise=0.03, light=True):
        """Paint a face from a list of strings; '.' (or chars missing from pal) are skipped."""
        x0, y0, w, h = self.rect(part, face)
        for y, row in enumerate(rows[:h]):
            for x, ch in enumerate(row[:w]):
                if ch in pal and pal[ch] is not None:
                    keep = ch.isupper() and ch in "WEIOX"  # bright eye pixels etc.
                    self.put(x0 + x, y0 + y, pal[ch], noise, face, light and not keep)

    def clear(self, part, face=None):
        for f in (FACES if face is None else (face,)):
            x0, y0, w, h = self.rect(part, f)
            for y in range(h):
                for x in range(w):
                    self.img.putpixel((x0 + x, y0 + y), (0, 0, 0, 0))
                    self.mat.pop((x0 + x, y0 + y), None)

    def finish(self):
        out = self.img.copy()
        for (x, y), n in self.mat.items():
            c = out.getpixel((x, y))
            if c[3] == 0: continue
            f = 1.0 if (x, y) in self.nolight else LIGHT.get(self.facemap.get((x, y), "front"), 1.0)
            f *= 1.0 + self.rng.uniform(-n, n)
            out.putpixel((x, y), shade(c, f))
        return out

def R(f, x, y):
    """Deterministic per-pixel hash used for fabric dithering (no visible diagonals)."""
    v = (x * 73856093) ^ (y * 19349663) ^ (sum(map(ord, f)) * 83492791)
    v = (v ^ (v >> 13)) * 1274126177
    return (v ^ (v >> 16)) & 0x7fffffff

def fill(c):
    return lambda face, x, y, w, h: c

def vgrad(top, bottom):
    return lambda face, x, y, w, h: mix(top, bottom, y / max(1, h - 1))

def preview(skin_img, slim, scale=6):
    """Front + back orthographic composite for eyeballing a skin."""
    P = parts(slim)
    aw = 3 if slim else 4
    W, H = (8 + 2 * aw) * 2 + 6, 32
    out = Image.new("RGBA", (W * scale, H * scale), (60, 70, 80, 255))
    def blit(part, face, dx, dy, layer2=None):
        for name in ([part] + ([layer2] if layer2 else [])):
            x0, y0, w, h = face_rect(*P[name], face)
            region = skin_img.crop((x0, y0, x0 + w, y0 + h)).resize((w * scale, h * scale), Image.NEAREST)
            out.alpha_composite(region, (dx * scale, dy * scale))
    def figure(ox, face):
        back = face == "back"
        blit("head", face, ox + aw, 0, "hat")
        blit("body", face, ox + aw, 8, "jacket")
        ra, la = ("rarm", "rsleeve"), ("larm", "lsleeve")
        rl, ll = ("rleg", "rpants"), ("lleg", "lpants")
        if back: ra, la, rl, ll = la, ra, ll, rl
        blit(ra[0], face, ox, 8, ra[1]); blit(la[0], face, ox + aw + 8, 8, la[1])
        blit(rl[0], face, ox + aw, 20, rl[1]); blit(ll[0], face, ox + aw + 4, 20, ll[1])
    figure(1, "front"); figure(1 + 8 + 2 * aw + 4, "back")
    return out
