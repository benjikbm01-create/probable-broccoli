"""Paints the original character skins for The Forgotten Island.
Run:  python3 make_skins.py <out_dir> [preview_dir]
All artwork here is original and released with the project (see ATTRIBUTION.md)."""
import sys, os
from skinlib import Skin, fill, mix, shade, hexc, preview, R

# ---------------------------------------------------------------- helpers
def streak_hair(base, dark, light, seed=0):
    """Hair texture: vertical strands with occasional highlights."""
    def fn(face, x, y, w, h):
        k = (x * 7 + seed * 3) % 5
        if face == "top":
            k = (x * 3 + y * 5 + seed) % 6
            return light if k == 0 else (dark if k == 3 else base)
        return light if (k == 0 and y % 3 != 2) else (dark if k == 3 else base)
    return fn

def head_skin(sk, skin, shadow):
    sk.paint("head", "all", lambda f, x, y, w, h: shadow if (f == "bottom" or (f != "top" and y == 7 and f != "front")) else skin, noise=0.02)

def hair_cover(sk, hair_fn, side_profile, back_rows, top=True):
    """side_profile[row] = number of columns (from the face side) left as skin on side faces.
    back_rows = number of rows from the top of the back face covered by hair (rest is nape)."""
    if top: sk.paint("head", "top", hair_fn, noise=0.04)
    for face in ("right", "left"):
        def fn(f, x, y, w, h):
            front_dist = (w - 1 - x) if f == "right" else x   # distance to the face edge
            return hair_fn(f, x, y, w, h) if front_dist >= side_profile[y] else None
        sk.paint("head", face, fn, noise=0.04)
    sk.paint("head", "back", lambda f, x, y, w, h: hair_fn(f, x, y, w, h) if y < back_rows else None, noise=0.04)

def ear(sk, skin_shadow, row=4, inset=3):
    for face in ("right", "left"):
        x0, y0, w, h = sk.rect("head", face)
        x = x0 + (w - 1 - inset if face == "right" else inset)
        sk.put(x, y0 + row, skin_shadow, 0.02, face)
        sk.put(x, y0 + row + 1, shade(skin_shadow, 0.95), 0.02, face)

def clothes(sk, part, fn, noise=0.06):
    sk.paint(part, "all", fn, noise=noise)

def boots(sk, color, height, sole="#2b2018", cuff=None):
    for leg in ("rleg", "lleg"):
        def fn(f, x, y, w, h, c=color):
            if f == "bottom": return sole
            if f == "top": return None
            if y == h - 1: return shade(sole, 1.2)
            if y >= h - height:
                if cuff and y == h - height: return cuff
                return c
            return None
        sk.paint(leg, "all", fn, noise=0.05)

def arms_skin(sk, skin, shadow, from_row=0):
    for arm in ("rarm", "larm"):
        sk.paint(arm, "all", lambda f, x, y, w, h: (shadow if f == "bottom" else skin) if y >= from_row or f == "bottom" else None, noise=0.02)

def hands(sk, skin, shadow, rows=1):
    for arm in ("rarm", "larm"):
        sk.paint(arm, "all", lambda f, x, y, w, h: (shadow if f == "bottom" else (skin if f != "top" else None)) if (f == "bottom" or y >= h - rows) else None, noise=0.02)

def face(sk, rows, pal):
    sk.grid("head", "front", rows, pal, noise=0.015)

def overlay(sk, part, face_, rows, pal, noise=0.03):
    sk.grid(part, face_, rows, pal, noise=noise)

# ================================================================ HESTER (innkeeper)
def hester():
    sk = Skin(slim=True, seed=11)
    s, S = "#ebbd9b", "#d39e7e"
    h, H, l = "#7b3f21", "#552a14", "#9c5a32"
    head_skin(sk, s, S)
    hf = streak_hair(h, H, l, 1)
    hair_cover(sk, hf, [8, 8, 2, 1, 1, 1, 0, 0], 6)
    ear(sk, S)
    face(sk, [
        "hhHhhHhh",
        "hlhhhhlh",
        "hsssssSh",
        "sHHssHHs",
        "sWessgWs".replace("g", "e"),
        "scsSSscs",
        "sssmmsss",
        "SssssssS"],
        {"h": h, "H": H, "l": l, "s": s, "S": S, "W": "#f2efe8", "e": "#5d7d58", "m": "#b0605a", "c": "#e8a28e"})
    # bun + loose strands (hat layer)
    overlay(sk, "hat", "back", [
        "........",
        "..lhhl..",
        ".hHhhHh.",
        ".hhHHhh.",
        "..hhhh..",
        "........",
        "........",
        "........"], {"h": h, "H": H, "l": l})
    overlay(sk, "hat", "front", [
        "lh....hl",
        "h......h",
        "h.......",
        "........",
        "........",
        "........",
        "........",
        "........"], {"h": h, "l": l})
    overlay(sk, "hat", "top", [
        "........", "........", "........", "........",
        "........", "...hh...", "..hlHh..", "..hHhh.."], {"h": h, "H": H, "l": l})
    # clothes: cream blouse, forest bodice, long brown skirt, white apron, silver locket
    blouse, bod, bodD = "#efe3c8", "#3f5a3a", "#2e4429"
    skirt, skirtD = "#6b4a33", "#563a27"
    apron = "#f3efe6"
    def body(f, x, y, w, h_):
        if f == "top": return blouse if x in (0, 7) or y in (0, 3) else bod
        if f == "bottom": return skirt
        if y <= 1:
            if f == "front" and y == 1 and x in (3, 4): return "#c9ccd4"   # locket chain
            return blouse
        if f == "front" and y == 2 and x in (3, 4): return "#dfe3ea"     # locket
        if y <= 7:
            if f == "front" and x in (2, 5) and y >= 3: return "#d9cdb0"  # lacing
            return bod if R(f, x, y) % 13 else bodD
        if y == 8: return "#3a2a1c"   # waist belt
        if f == "front" and 1 <= x <= 6: return apron if y < 11 else shade(apron, 0.93)
        return skirt
    clothes(sk, "body", body)
    def arm(f, x, y, w, h_):
        if f == "top": return blouse
        if y >= 11: return s if f != "bottom" else S
        if y == 7: return "#d8c9a8"   # rolled cuff
        if y > 7: return s
        return blouse if R(f, x, y) % 11 else shade(blouse, 0.94)
    clothes(sk, "rarm", arm, 0.03); clothes(sk, "larm", arm, 0.03)
    def leg(f, x, y, w, h_):
        if f == "top": return skirt
        if f == "bottom": return "#2a1d14"
        if y >= 10: return "#3b2a1e" if y == 11 else "#4a3424"   # boots peek out
        if f == "front" and y < 4: return apron
        return skirt if R(f, x, y) % 15 else skirtD
    clothes(sk, "rleg", leg); clothes(sk, "lleg", leg)
    # skirt flare on the pants layer (lower rows only) + apron overlay
    def flare(f, x, y, w, h_):
        if f in ("top", "bottom") or y < 5 or y > 9: return None
        if f == "front" and y < 8: return shade(apron, 0.95)
        return skirtD if y == 9 else skirt
    sk.paint("rpants", "all", flare, noise=0.05); sk.paint("lpants", "all", flare, noise=0.05)
    def apron_o(f, x, y, w, h_):
        if f == "front" and 9 <= y <= 11 and 1 <= x <= 6: return shade(apron, 0.97)
        if f == "front" and y == 8 and x in (1, 6): return "#e8e2d4"
        if f in ("left", "right") and y == 8: return "#e8e2d4"   # apron strings
        if f == "back" and y == 8 and x in (3, 4): return "#e8e2d4"
        return None
    sk.paint("jacket", "all", apron_o, noise=0.03)
    return sk

# ================================================================ TOBIAS (harbormaster)
def tobias():
    sk = Skin(slim=False, seed=21)
    s, S = "#c98f68", "#a87252"
    g, G, gl = "#9a9a96", "#6f6f6c", "#c4c4bf"
    head_skin(sk, s, S)
    hf = streak_hair(g, G, gl, 2)
    hair_cover(sk, hf, [8, 8, 8, 2, 2, 1, 1, 1], 7)
    ear(sk, S, 3, 2)
    face(sk, [
        "kkkkkkkk",
        "kkkkkkkk",
        "ssSssSss",
        "sgGssGgs",
        "sWeSSeWs",
        "sssSSsss",
        "gggmmggg",
        "gGggggGg"],
        {"k": "#1f2a44", "s": s, "S": S, "g": g, "G": G, "W": "#ebe6da", "e": "#4d6b8a", "m": "#7a4a3c"})
    # beard wraps to the sides
    for f_ in ("right", "left"):
        overlay(sk, "head", f_, ["", "", "", "", "", "", "gggggggg"[:8], "GgggggGg"],
                {"g": g, "G": G})
    # captain's cap on hat layer
    navy, navyD, brim, brass = "#23304f", "#18223a", "#141414", "#c9a24a"
    sk.paint("hat", "all", lambda f, x, y, w, h: (navy if R(f, x, y) % 11 else navyD) if (f == "top" or y <= 1) else None, noise=0.04)
    overlay(sk, "hat", "front", ["kkkkkkkk", "bbbbbbbb", "..BBBB.."], {"k": navy, "b": brim, "B": brim})
    sk.put(*(lambda r: (r[0] + 3, r[1] + 0))(sk.rect("hat", "front")), brass, 0.02, "front", False)
    sk.put(*(lambda r: (r[0] + 4, r[1] + 0))(sk.rect("hat", "front")), brass, 0.02, "front", False)
    # peacoat
    coat, coatD = "#26355a", "#1c2846"
    def body(f, x, y, w, h_):
        if f == "top": return coat
        if f == "bottom": return coatD
        if f == "front":
            if y <= 1 and x in (3, 4): return "#e9e4d6"   # shirt collar
            if y <= 2 and x in (2, 5): return coatD       # lapels
            if x in (2, 5) and y in (4, 7, 10): return brass
            if x in (3, 4): return coatD if y > 1 else coat
        if y == 8: return "#3b2a1c" if f != "front" or x not in (3, 4) else "#b08a3a"
        return coat if R(f, x, y) % 15 else coatD
    clothes(sk, "body", body, 0.05)
    def arm(f, x, y, w, h_):
        if y >= 11: return s if f != "bottom" else S
        if y == 10: return coatD
        if f == "top": return coat
        if y == 9 and f == "front": return brass if x == 1 else coat
        return coat if R(f, x, y) % 13 else coatD
    clothes(sk, "rarm", arm); clothes(sk, "larm", arm)
    tr, trD = "#3a3a42", "#2c2c33"
    clothes(sk, "rleg", lambda f, x, y, w, h_: coat if y < 2 else (tr if R(f, x, y) % 11 else trD))
    clothes(sk, "lleg", lambda f, x, y, w, h_: coat if y < 2 else (tr if R(f, x, y) % 11 else trD))
    boots(sk, "#2a1e16", 4, "#150f0b", "#3a2a1e")
    # coat skirt overlay on jacket layer bottom rows and pants top rows
    sk.paint("jacket", ("front", "back", "left", "right"), lambda f, x, y, w, h_: coatD if y >= 10 and not (f == "front" and x in (3, 4)) else None)
    sk.paint("rpants", ("front", "back", "right", "left"), lambda f, x, y, w, h_: coat if y < 3 and not (f == "front" and x == 3) else None)
    sk.paint("lpants", ("front", "back", "right", "left"), lambda f, x, y, w, h_: coat if y < 3 and not (f == "front" and x == 0) else None)
    return sk

# ================================================================ WENDEL (old fisherman)
def wendel():
    sk = Skin(slim=False, seed=31)
    s, S = "#e3b9a0", "#c89a82"
    w_, W_, wl = "#e6e3dc", "#b9b6ae", "#fbfaf6"
    head_skin(sk, s, S)
    # bald crown, white hair ring around the sides/back
    sk.paint("head", "top", lambda f, x, y, w, h: (w_ if (x in (0, 7) or y == 7) else (S if R(f, x, y) % 15 == 0 else s)), noise=0.03)
    hf = streak_hair(w_, W_, wl, 3)
    for f_ in ("right", "left"):
        sk.paint("head", f_, lambda f, x, y, w, h: hf(f, x, y, w, h) if (y >= 2 and ((w - 1 - x) if f == "right" else x) >= 3) else None, noise=0.04)
    sk.paint("head", "back", lambda f, x, y, w, h: hf(f, x, y, w, h) if 1 <= y <= 6 else None)
    ear(sk, S, 4, 2)
    face(sk, [
        "sSsssSss",
        "sssSssss",
        "wwwssWww",
        "sWWssWWs".replace("W", "w"),
        "sXeSSeXs",
        "sSssssSs",
        "wwwmmwww",
        "wwWwwWww"],
        {"s": s, "S": S, "w": w_, "W": W_, "X": "#e8e4da", "e": "#6a7f8c", "m": "#8c5a50"})
    overlay(sk, "hat", "front", ["", "", "", "", "", "", "w......w", "wwwWWwww"], {"w": w_, "W": W_})
    for f_ in ("right", "left"):
        overlay(sk, "hat", f_, ["", "", "", "", "", "", "", "wwww...." if f_ == "left" else "....wwww"], {"w": w_})
    # patched cardigan, wool scarf
    card, cardD, patch = "#7d5b3e", "#644731", "#8e8a5a"
    scarf = "#a6483c"
    def body(f, x, y, w, h_):
        if f == "top": return scarf if y in (1, 2) and 1 < x < 6 else card
        if y <= 1: return scarf if f != "bottom" else card
        if f == "front" and y in (2, 3, 4, 5) and x == 5: return shade(scarf, 0.9)   # hanging end
        if f == "front" and x in (3, 4): return "#d8ccb4" if x == 3 else cardD
        if f == "front" and 1 <= x <= 2 and 8 <= y <= 10: return patch
        if f == "back" and 4 <= x <= 6 and 4 <= y <= 6: return patch
        return card if R(f, x, y) % 9 else cardD    # knit texture
    clothes(sk, "body", body, 0.06)
    def arm(f, x, y, w, h_):
        if y >= 11: return s if f != "bottom" else S
        if f == "top": return card
        if f in ("front", "left") and 5 <= y <= 6 and x == 1: return patch
        return card if R(f, x, y) % 9 else cardD
    clothes(sk, "rarm", arm); clothes(sk, "larm", arm)
    tr = "#4f4a42"
    clothes(sk, "rleg", lambda f, x, y, w, h_: tr if R(f, x, y) % 13 else shade(tr, 0.85))
    clothes(sk, "lleg", lambda f, x, y, w, h_: tr if R(f, x, y) % 13 else shade(tr, 0.85))
    boots(sk, "#3b2a1d", 3, "#1e150e")
    sk.paint("jacket", ("front", "back", "left", "right"), lambda f, x, y, w, h_: scarf if y <= 1 else None)
    return sk

# ================================================================ CORIN (blacksmith)
def corin():
    sk = Skin(slim=False, seed=41)
    s, S = "#d29a76", "#b27a58"
    b, B, bl = "#2e231c", "#1c1510", "#43342a"
    head_skin(sk, s, S)
    # shaved head: stubble tone on top/back
    stub = mix(s, b, 0.35)
    sk.paint("head", "top", lambda f, x, y, w, h: stub if R(f, x, y) % 7 else shade(stub, 0.93), noise=0.04)
    sk.paint("head", "back", lambda f, x, y, w, h: stub if y < 6 else (b if y >= 6 and False else None), noise=0.04)
    for f_ in ("right", "left"):
        sk.paint("head", f_, lambda f, x, y, w, h: (stub if y < 2 else (b if y >= 5 and ((w - 1 - x) if f == "right" else x) < 4 else None)), noise=0.04)
    ear(sk, S, 3, 3)
    face(sk, [
        "TTTTTTTT",
        "sssSSsss",
        "sBBssBBs",
        "sWessewS".replace("w", "W").replace("S", "s"),
        "ssSssSss",
        "bbSSSSbb",
        "bbbmmbbb",
        "BbbbbbbB"],
        {"T": stub, "s": s, "S": S, "b": b, "B": B, "W": "#efe9df", "e": "#5a3c28", "m": "#6e3a30"})
    # soot smudge on cheek
    fx, fy, _, _ = sk.rect("head", "front"); sk.put(fx + 6, fy + 4, shade(s, 0.78), 0.02)
    shirt, apronc, apronD = "#d9cfb9", "#5b3b22", "#472d19"
    def body(f, x, y, w, h_):
        if f == "top": return shirt if y == 0 or y == 3 else apronc
        if f == "bottom": return apronD
        if f == "front":
            if y <= 1: return s if x in (3, 4) and y == 0 else shirt
            if x in (1, 6) and y == 2: return "#3a2614"   # apron straps
            if y >= 2: return apronc if R(f, x, y) % 11 else apronD
        if f == "back" and x in (2, 5) and y <= 7: return "#3a2614"   # crossed straps
        if y == 7: return "#3a2614"
        return shirt if R(f, x, y) % 13 else shade(shirt, 0.92)
    clothes(sk, "body", body, 0.06)
    def arm(f, x, y, w, h_):
        if f == "top": return shirt
        if y <= 3: return shirt if y < 3 else "#c7bba2"   # rolled sleeves
        if y >= 9: return "#3f2c1e" if f != "bottom" else "#2f2116"   # thick gloves
        return s if R(f, x, y) % 19 else S
    clothes(sk, "rarm", arm, 0.03); clothes(sk, "larm", arm, 0.03)
    tr = "#3e3a36"
    clothes(sk, "rleg", lambda f, x, y, w, h_: (apronc if f == "front" and y < 5 else (tr if R(f, x, y) % 11 else shade(tr, 0.85))))
    clothes(sk, "lleg", lambda f, x, y, w, h_: (apronc if f == "front" and y < 5 else (tr if R(f, x, y) % 11 else shade(tr, 0.85))))
    boots(sk, "#2a1d12", 4, "#140d08", "#3d2b1c")
    sk.paint("jacket", "front", lambda f, x, y, w, h_: ("#7a7a7a" if (x, y) in ((2, 6), (5, 9)) else None))  # rivets / tools
    return sk

# ================================================================ WREN (ranger)  -- romance, most detail
def wren():
    sk = Skin(slim=True, seed=51)
    s, S = "#f0c7a8", "#d9a688"
    h, H, l = "#b4532a", "#86381a", "#d9773f"   # copper-auburn
    head_skin(sk, s, S)
    hf = streak_hair(h, H, l, 4)
    hair_cover(sk, hf, [8, 8, 3, 2, 1, 1, 1, 1], 8)
    ear(sk, S, 4, 2)
    face(sk, [
        "hlhhhHhh",
        "hhlhhhlh",
        "hHsssshh",
        "sHHssHHs",
        "sWeSsEWs".replace("E", "e"),
        "sfsSSsfs",
        "sssmmsss",
        "SsssssSS"],
        {"h": h, "H": H, "l": l, "s": s, "S": S, "W": "#f6f3ec", "e": "#3f8a52", "f": "#cf8a62", "m": "#bb5a4e"})
    # freckles across the nose
    fx, fy, _, _ = sk.rect("head", "front")
    for px in (2, 5): sk.put(fx + px, fy + 5, "#d2916a", 0.01)
    # side-swept fringe + braid start on hat layer
    overlay(sk, "hat", "front", [
        "hlhhh...",
        "lh......",
        "h.......",
        "h.......",
        "........", "........", "........", "........"], {"h": h, "l": l})
    overlay(sk, "hat", "top", [
        "hhlhhhlh", "h.......", "l......h", "h.......", "h......h", "........", "h......l", "hhlhhhhh"], {"h": h, "l": l})
    overlay(sk, "hat", "back", [
        "hhlhhhlh",
        "h......h",
        "........",
        "...HH...",
        "...hl...",
        "...Hh...",
        "...hH...",
        "...lh..."], {"h": h, "H": H, "l": l})
    overlay(sk, "hat", "right", ["hhhhhhhh", "h.......", "h.......", "........", "", "", "", ""], {"h": h})
    overlay(sk, "hat", "left", ["hhhhhhhh", ".......h", ".......h", "........", "", "", "", ""], {"h": h})
    # braid hangs down the back (jacket layer back) and a leather tie
    cloak, cloakD, cloakL = "#2f5a34", "#234527", "#3f7445"
    leather, leatherD = "#7a5233", "#5e3e25"
    strap = "#4a2f1b"
    def jacket(f, x, y, w, h_):
        if f == "back":
            if 3 <= x <= 4 and y <= 6: return [h, l, H, h, l, H, h][y] if x == 3 else [H, h, l, H, h, l, "#4a2f1b"][y]
            if y <= 1: return cloakD                                 # hood folded on shoulders
            return None
        if f in ("left", "right") and y <= 1: return cloakD
        if f == "front" and y == 0 and x in (0, 1, 6, 7): return cloakD
        if f == "top": return cloakD if x in (0, 1, 6, 7) else None
        return None
    sk.paint("jacket", "all", jacket, noise=0.04)
    def body(f, x, y, w, h_):
        if f == "top": return cloak if x in (0, 1, 6, 7) else leather
        if f == "bottom": return "#2f3b25"
        if f == "front":
            if (x + 8 - y) in (9, 10) and 1 <= y <= 9: return strap          # quiver strap diagonal
            if y == 0: return "#e2d5b5" if x in (3, 4) else cloak             # shirt collar
            if y == 8: return "#3b2716" if x not in (3, 4) else "#b9a06a"     # belt + buckle
            if y > 8: return leatherD
            return leather if R(f, x, y) % 11 else leatherD
        if f == "back":
            if (x + y) in (5, 6) and 1 <= y <= 9: return strap
            if y >= 2 and 4 <= x <= 6 and y <= 9: return "#6d4a2d" if y > 3 else "#8a6a46"   # quiver
            if y == 8: return "#3b2716"
            return cloak if R(f, x, y) % 13 else cloakD
        if y == 8: return "#3b2716"
        return cloak if R(f, x, y) % 13 else cloakD
    clothes(sk, "body", body, 0.05)
    # arrows fletching sticking out of quiver on top of body back-jacket
    jx, jy, _, _ = sk.rect("jacket", "back")
    for (dx, c) in ((5, "#e8e0cc"), (6, "#c0392b")): sk.put(jx + dx, jy + 1, c, 0.02, "back")
    def arm(f, x, y, w, h_):
        if f == "top": return cloak
        if y <= 3: return "#e2d5b5"                      # linen shirt sleeve
        if 4 <= y <= 9: return leatherD if y in (4, 9) else leather   # bracers
        return s if f != "bottom" else S
    clothes(sk, "rarm", arm, 0.04); clothes(sk, "larm", arm, 0.04)
    sk.paint("rsleeve", "all", lambda f, x, y, w, h_: cloakD if (y <= 2 and f != "bottom") else None)
    sk.paint("lsleeve", "all", lambda f, x, y, w, h_: cloakD if (y <= 2 and f != "bottom") else None)
    lg, lgD = "#38452c", "#2b3622"
    clothes(sk, "rleg", lambda f, x, y, w, h_: lg if R(f, x, y) % 11 else lgD)
    clothes(sk, "lleg", lambda f, x, y, w, h_: lg if R(f, x, y) % 11 else lgD)
    boots(sk, "#5a3a22", 6, "#241810", "#7a5233")
    # cloak tail on pants layer back
    sk.paint("rpants", "back", lambda f, x, y, w, h_: cloakD if y < 4 else None)
    sk.paint("lpants", "back", lambda f, x, y, w, h_: cloakD if y < 4 else None)
    return sk

# ================================================================ IONE (archivist) -- romance
def ione():
    sk = Skin(slim=True, seed=61)
    s, S = "#e6c0a0", "#cca184"
    h, H, l = "#22191a", "#120c0d", "#3b2d2e"
    head_skin(sk, s, S)
    hf = streak_hair(h, H, l, 5)
    hair_cover(sk, hf, [8, 8, 3, 2, 1, 1, 1, 0], 6)
    ear(sk, S, 4, 2)
    face(sk, [
        "hhhhhhhh",
        "hlhhhlhh",
        "hssssssh",
        "shHssHhs",
        "sWessewS".replace("w", "W").replace("S", "s"),
        "scsSSscs",
        "sssmmsss",
        "SssssssS"],
        {"h": h, "H": H, "l": l, "s": s, "S": S, "W": "#f3f1ec", "e": "#5d7488", "m": "#a8545a", "c": "#e2a594"})
    # round glasses on hat layer front
    frame = "#8a6a2a"
    overlay(sk, "hat", "front", [
        "........",
        "h.......",
        "........",
        ".gg..gg.",
        "g..gg..g",
        ".gg..gg.",
        "........", "........"], {"g": "#6b4f22", "h": l})
    # messy bun + pencil
    overlay(sk, "hat", "top", ["........", "........", "........", "........", "...hh...", "..hlHh..", "..hHhh..", "...hh..."], {"h": h, "H": H, "l": l})
    overlay(sk, "hat", "back", ["..hhhh..", ".hlHhhh.", ".hHhlhh.", "..hhhh..", "...hh...", "", "", ""], {"h": h, "H": H, "l": l})
    bx, by, _, _ = sk.rect("hat", "back")
    sk.put(bx + 6, by + 0, "#e3b23c", 0.02, "back"); sk.put(bx + 7, by + 0, "#f0d9a0", 0.02, "back")   # pencil
    overlay(sk, "hat", "right", ["", "", "h.......", "h.......", "", "", "", ""], {"h": h})
    overlay(sk, "hat", "left", ["", "", ".......h", ".......h", "", "", "", ""], {"h": h})
    coat, coatD, coatL = "#3e4f6e", "#2f3d57", "#52668a"
    shirt, scarf = "#f1ede4", "#7c2f3c"
    brass = "#c8a248"
    def body(f, x, y, w, h_):
        if f == "top": return scarf if 2 <= x <= 5 and 1 <= y <= 2 else coat
        if f == "bottom": return coatD
        if f == "front":
            if y <= 1 and 2 <= x <= 5: return scarf if y == 0 else (scarf if x in (3, 4) else shirt)
            if 2 <= y <= 4 and x == 3: return shade(scarf, 0.9)
            if x in (3, 4) and y >= 2: return shirt if x == 4 else coatD
            if x == 2 and y in (3, 6, 9): return brass
            if y == 7: return "#2a2622"
            return coat if R(f, x, y) % 13 else coatL
        if y <= 0: return scarf
        if y == 7: return "#2a2622"
        return coat if R(f, x, y) % 13 else coatD
    clothes(sk, "body", body, 0.05)
    def arm(f, x, y, w, h_):
        if f == "top": return coat
        if y >= 11: return s if f != "bottom" else S
        if y == 10: return shirt                         # shirt cuff
        if y == 9: return coatL
        return coat if R(f, x, y) % 13 else coatD
    clothes(sk, "rarm", arm); clothes(sk, "larm", arm)
    # ink stains on right hand
    rx, ry, _, _ = sk.rect("rarm", "front"); sk.put(rx + 1, ry + 11, "#2c3a66", 0.02)
    lx_, ly_, _, _ = sk.rect("rarm", "bottom"); sk.put(lx_ + 0, ly_ + 1, "#2c3a66", 0.02, "bottom")
    sk_c, sk_d = "#2d2a33", "#221f27"
    clothes(sk, "rleg", lambda f, x, y, w, h_: sk_c if R(f, x, y) % 11 else sk_d)
    clothes(sk, "lleg", lambda f, x, y, w, h_: sk_c if R(f, x, y) % 11 else sk_d)
    boots(sk, "#4b2e22", 5, "#1d120d", "#5b3a2c")
    # long coat tails on pants layer
    tail = lambda f, x, y, w, h_: (coat if R(f, x, y) % 13 else coatD) if (y < 6 and f in ("back", "left", "right", "front") and not (f == "front" and y > 1)) else None
    sk.paint("rpants", "all", tail); sk.paint("lpants", "all", tail)
    sk.paint("jacket", ("front", "back", "left", "right"), lambda f, x, y, w, h_: (coatD if (f != "front" or x in (0, 1, 6, 7)) else None) if y >= 9 else None)
    # collar
    sk.paint("jacket", ("front", "left", "right", "back"), lambda f, x, y, w, h_: coatL if y == 0 and not (f == "front" and 2 <= x <= 5) else None)
    return sk

# ================================================================ SOL (shipwright) -- romance
def sol():
    sk = Skin(slim=True, seed=71)
    s, S = "#9c6542", "#82512f"
    h, H, l = "#1d1410", "#0e0907", "#3a2a20"
    head_skin(sk, s, S)
    hf = streak_hair(h, H, l, 6)
    hair_cover(sk, hf, [8, 8, 2, 1, 1, 1, 0, 0], 7)
    ear(sk, S, 4, 2)
    band, bandD = "#c0392b", "#8e2a20"
    face(sk, [
        "bbbbbbbb",
        "hlhbBhlh".replace("b", "h").replace("B", "h"),
        "hssssssh",
        "sHHssHHs",
        "sWessewS".replace("w", "W").replace("S", "s"),
        "sSsSSsSs",
        "sssmmsss",
        "SssssssS"],
        {"b": band, "B": bandD, "h": h, "H": H, "l": l, "s": s, "S": S, "W": "#f2ede4", "e": "#7a4b1f", "m": "#7e3a36"})
    # bandana wraps the head, knot at back
    for f_ in ("right", "left", "back"):
        overlay(sk, "head", f_, ["bbbbbbbb"], {"b": band})
    overlay(sk, "head", "top", ["bbbbbbbb", "bBbbbbBb", "bbbbbbbb", "bbbBbbbb", "bbbbbbbb", "bbbbbBbb", "bbbbbbbb", "bbbbbbbb"], {"b": band, "B": bandD})
    # curls (hat layer) bursting out under the bandana
    overlay(sk, "hat", "front", ["........", "l.h..h.l", "h......h", "h......h", "", "", "", ""], {"h": h, "l": l})
    overlay(sk, "hat", "right", ["........", "hlhhlhhl", "hhHhhlh.", "lhhHh...", "hhh.....", "hh......", "", ""], {"h": h, "H": H, "l": l})
    overlay(sk, "hat", "left", ["........", "lhhlhhlh", ".hlhhHhh", "...hHhhl", ".....hhh", "......hh", "", ""], {"h": h, "H": H, "l": l})
    overlay(sk, "hat", "back", ["...bb...", "hlbBBblh", "hhHbbhHh", "lhhHhhlh", "hhlhhhhh", "hHhhlhHh", ".hhhhhh.", "..h..h.."], {"h": h, "H": H, "l": l, "b": band, "B": bandD})
    # gold hoop earring
    rx, ry, rw, _ = sk.rect("hat", "right"); sk.put(rx + rw - 3, ry + 6, "#e2b84a", 0.0, "right", False)
    lx, ly, _, _ = sk.rect("hat", "left"); sk.put(lx + 2, ly + 6, "#e2b84a", 0.0, "left", False)
    shirt, shirtD = "#f4efe3", "#ddd5c4"
    vest, vestD = "#b0844f", "#8d6639"
    sash = "#c0392b"
    def body(f, x, y, w, h_):
        if f == "top": return vest if x in (0, 1, 6, 7) else shirt
        if f == "bottom": return "#1f4a4f"
        if f == "front":
            if y <= 2 and 3 <= x <= 4: return s if y < 2 else shirtD           # open collar
            if x in (0, 1, 6, 7) and y <= 7: return vest if R(f, x, y) % 11 else vestD
            if y in (8, 9): return sash if not (x == 5 and y == 9) else shade(sash, 0.8)
            if y >= 10: return "#1f4a4f"
            return shirt if (y % 3) else shirtD
        if y in (8, 9): return sash
        if y >= 10: return "#1f4a4f"
        return vest if R(f, x, y) % 11 else vestD
    clothes(sk, "body", body, 0.05)
    # sash tail hanging on jacket layer
    sk.paint("jacket", "front", lambda f, x, y, w, h_: shade(sash, 0.9) if x == 6 and 9 <= y <= 11 else None)
    def arm(f, x, y, w, h_):
        if f == "top": return shirt
        if y <= 4: return shirt if y < 4 else shirtD     # rolled sleeves
        return s if f != "bottom" else S
    clothes(sk, "rarm", arm, 0.03); clothes(sk, "larm", arm, 0.03)
    # anchor tattoo on the right forearm
    ax, ay, _, _ = sk.rect("rarm", "front")
    for (dx, dy) in ((1, 6), (1, 7), (1, 8), (0, 7), (2, 7), (0, 9), (2, 9)): sk.put(ax + dx, ay + dy, "#3b3f5c", 0.02)
    # leather bracelet left wrist
    sk.paint("larm", ("front", "left", "right", "back"), lambda f, x, y, w, h_: "#5a3a22" if y == 9 else None)
    tr, trD = "#1f4a4f", "#173a3e"
    clothes(sk, "rleg", lambda f, x, y, w, h_: (tr if R(f, x, y) % 11 else trD) if y < 9 else (s if y < 11 else "#3a2a1e"))
    clothes(sk, "lleg", lambda f, x, y, w, h_: (tr if R(f, x, y) % 11 else trD) if y < 9 else (s if y < 11 else "#3a2a1e"))
    for leg in ("rleg", "lleg"):
        sk.paint(leg, ("front", "back", "left", "right"), lambda f, x, y, w, h_: shade(tr, 1.15) if y == 8 else None)   # rolled hem
        sk.paint(leg, "bottom", fill("#2a1d14"))
    return sk

# ================================================================ THE DROWNED WARDEN (boss)
def warden():
    sk = Skin(slim=False, seed=81)
    s, S = "#8ea3a6", "#6f8486"
    h, H, l = "#2c3434", "#1a2020", "#3e4a49"
    head_skin(sk, s, S)
    hf = streak_hair(h, H, l, 7)
    hair_cover(sk, hf, [8, 8, 3, 2, 2, 2, 2, 2], 8)
    face(sk, [
        "hhHhhhHh",
        "hHhsshhH",
        "hssssssh",
        "sHHssHHs",
        "sOossoOs",
        "sSsSSsSs",
        "hssmmssh",
        "hHhhhhHh"],
        {"h": h, "H": H, "l": l, "s": s, "S": S, "O": "#7ff3ff", "o": "#2fb8c8", "m": "#3e4a4a"})
    overlay(sk, "hat", "front", ["hh.hh.hh", "h......h", "h......h", "", "", "", "h......h", "hh....hh"], {"h": h})
    kelp = "#3f6b2b"
    overlay(sk, "hat", "right", ["hhhhhhhh", "hhh.....", "hk......", "hk......", "k.......", "", "", ""], {"h": h, "k": kelp})
    overlay(sk, "hat", "left", ["hhhhhhhh", ".....hhh", "......kh", "......kh", ".......k", "", "", ""], {"h": h, "k": kelp})
    coat, coatD, brass = "#2a3550", "#1b2236", "#8a7a46"
    def body(f, x, y, w, h_):
        if f == "top": return coat
        if f in ("front", "back") and (R(f, x, y) % 23 == 0): return kelp
        if f == "front":
            if x in (3, 4) and y < 6: return "#9aa6a0" if y < 2 else coatD
            if x in (2, 5) and y in (3, 6, 9): return brass
            if y > 8 and x in (1, 6) and y % 2: return None or "#3a5a5c"
        if y == 8: return "#2a2016"
        return coat if R(f, x, y) % 11 else coatD
    clothes(sk, "body", body)
    def arm(f, x, y, w, h_):
        if y >= 11: return s if f != "bottom" else S
        if f in ("front", "back") and y == 7: return kelp
        return coat if R(f, x, y) % 11 else coatD
    clothes(sk, "rarm", arm); clothes(sk, "larm", arm)
    clothes(sk, "rleg", lambda f, x, y, w, h_: "#252a30" if R(f, x, y) % 9 else kelp if y > 6 else "#1c2026")
    clothes(sk, "lleg", lambda f, x, y, w, h_: "#252a30" if R(f, x, y) % 9 else kelp if y > 6 else "#1c2026")
    boots(sk, "#1d1915", 4, "#0f0c0a")
    # tattered coat edges and dripping kelp on overlay layers
    sk.paint("jacket", ("front", "back"), lambda f, x, y, w, h_: (coatD if y >= 10 and x % 3 != 1 else (kelp if (x, y) in ((1, 3), (6, 5), (2, 9)) else None)))
    sk.paint("rsleeve", ("front", "back", "right", "left"), lambda f, x, y, w, h_: kelp if (y in (2, 8) and x % 2 == 0) else None)
    sk.paint("lsleeve", ("front", "back", "right", "left"), lambda f, x, y, w, h_: kelp if (y in (3, 9) and x % 2 == 1) else None)
    return sk

CHARS = {"hester": hester, "tobias": tobias, "wendel": wendel, "corin": corin,
         "wren": wren, "ione": ione, "sol": sol, "warden": warden}

if __name__ == "__main__":
    out = sys.argv[1]; prev = sys.argv[2] if len(sys.argv) > 2 else None
    os.makedirs(out, exist_ok=True)
    sheet = []
    for name, fn in CHARS.items():
        sk = fn()
        img = sk.finish()
        img.save(os.path.join(out, name + ".png"))
        if prev:
            os.makedirs(prev, exist_ok=True)
            sheet.append(preview(img, sk.slim))
    if prev:
        from PIL import Image
        W = sum(p.width for p in sheet) + 10 * len(sheet); H = max(p.height for p in sheet)
        board = Image.new("RGBA", (W, H), (40, 44, 52, 255)); x = 0
        for p in sheet: board.alpha_composite(p, (x, 0)); x += p.width + 10
        board.save(os.path.join(prev, "all_skins.png"))
    print("skins:", ", ".join(CHARS))
