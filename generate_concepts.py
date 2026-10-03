import math
from PIL import Image, ImageDraw, ImageFilter

def create_option_a_apex_prism(size=1024):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    # 1. Background Squircle
    pad = int(48 * s)
    corner_r = int(220 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    bg = Image.new("RGBA", (W, H), (10, 15, 26, 255))
    
    # Subtle ambient gradient
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    for r in range(int(450 * s), 0, -10):
        a = int(35 * (1.0 - (r / (450 * s)) ** 1.3))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(6, 182, 212, a))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=20 * s))
    bg = Image.alpha_composite(bg, glow)
    
    # Subtle border
    b_draw = ImageDraw.Draw(bg)
    b_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 230), width=int(3 * s))
    img.paste(bg, (0, 0), bg_mask)
    
    # 2. Hero Shield - Precision Proportions
    # Top center apex: (cx, cy - 310*s)
    # Top left shoulder: (cx - 240*s, cy - 190*s)
    # Top right shoulder: (cx + 240*s, cy - 190*s)
    # Mid-low waist left: (cx - 230*s, cy + 80*s)
    # Mid-low waist right: (cx + 230*s, cy + 80*s)
    # Bottom point: (cx, cy + 340*s)
    
    # Shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    shield_poly = [
        (cx, cy - 310 * s),
        (cx + 240 * s, cy - 190 * s),
        (cx + 230 * s, cy + 80 * s),
        (cx, cy + 340 * s),
        (cx - 230 * s, cy + 80 * s),
        (cx - 240 * s, cy - 190 * s)
    ]
    s_draw.polygon(shield_poly, fill=(0, 0, 0, 160))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=30 * s))
    img = Image.alpha_composite(img, shadow)
    
    mark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Left Half of Shield (Deep Midnight Sapphire)
    left_shield = [
        (cx, cy - 310 * s),
        (cx - 240 * s, cy - 190 * s),
        (cx - 230 * s, cy + 80 * s),
        (cx, cy + 340 * s),
        (cx, cy - 310 * s)
    ]
    m_draw.polygon(left_shield, fill=(2, 132, 199, 255)) # Vibrant Sky/Sapphire
    
    # Right Half of Shield (Electric Cyan Luminescence)
    right_shield = [
        (cx, cy - 310 * s),
        (cx + 240 * s, cy - 190 * s),
        (cx + 230 * s, cy + 80 * s),
        (cx, cy + 340 * s),
        (cx, cy - 310 * s)
    ]
    m_draw.polygon(right_shield, fill=(6, 182, 212, 255)) # Electric Cyan
    
    # Right specular facet
    right_specular = [
        (cx, cy - 310 * s),
        (cx + 240 * s, cy - 190 * s),
        (cx, cy - 40 * s),
        (cx, cy - 310 * s)
    ]
    m_draw.polygon(right_specular, fill=(34, 211, 238, 255)) # Highlight Cyan
    
    # Left shadow facet
    left_shadow = [
        (cx, cy + 340 * s),
        (cx - 230 * s, cy + 80 * s),
        (cx, cy + 60 * s),
        (cx, cy + 340 * s)
    ]
    m_draw.polygon(left_shadow, fill=(3, 105, 161, 255)) # Deep Ocean Blue
    
    # 3. Negative Space & White Vanguard Monogram "A"
    # An imposing, razor-sharp stylized "A" forged directly in the core
    # Outer cut:
    cutout = [
        (cx, cy - 200 * s),
        (cx + 140 * s, cy + 120 * s),
        (cx + 70 * s, cy + 120 * s),
        (cx, cy - 40 * s),
        (cx - 70 * s, cy + 120 * s),
        (cx - 140 * s, cy + 120 * s)
    ]
    m_draw.polygon(cutout, fill=(10, 15, 26, 255))
    
    # Floating Platinum Spearpoint (The Crossbar / Core Diamond of the "A")
    core_diamond = [
        (cx, cy - 90 * s),
        (cx + 42 * s, cy + 10 * s),
        (cx, cy + 70 * s),
        (cx - 42 * s, cy + 10 * s)
    ]
    m_draw.polygon(core_diamond, fill=(248, 250, 252, 255)) # Pristine Platinum White
    
    # Split center vertical line for mechanical elegance
    m_draw.line([(cx, cy - 310 * s), (cx, cy - 200 * s)], fill=(255, 255, 255, 220), width=int(3 * s))
    m_draw.line([(cx, cy + 120 * s), (cx, cy + 340 * s)], fill=(255, 255, 255, 220), width=int(3 * s))
    
    # Precision outer rim stroke
    m_draw.polygon(shield_poly, outline=(248, 250, 252, 230), width=int(2.5 * s))
    
    img = Image.alpha_composite(img, mark)
    return img.resize((size, size), Image.Resampling.LANCZOS)

def create_option_b_cyber_monogram(size=1024):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    pad = int(48 * s)
    corner_r = int(220 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    bg = Image.new("RGBA", (W, H), (8, 12, 20, 255))
    
    # Ultra-clean radial glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    for r in range(int(460 * s), 0, -12):
        a = int(30 * (1.0 - (r / (460 * s)) ** 1.5))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(16, 185, 129, a)) # Emerald + Cyan
    glow = glow.filter(ImageFilter.GaussianBlur(radius=25 * s))
    bg = Image.alpha_composite(bg, glow)
    
    b_draw = ImageDraw.Draw(bg)
    b_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 200), width=int(3 * s))
    img.paste(bg, (0, 0), bg_mask)
    
    # Minimalist Architectural Shield Mark
    # Double-layered ribbon shield
    mark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Outer Bold Geometric Shield Path
    p_top = (cx, cy - 290 * s)
    p_tr = (cx + 230 * s, cy - 170 * s)
    p_mr = (cx + 220 * s, cy + 90 * s)
    p_bot = (cx, cy + 330 * s)
    p_ml = (cx - 220 * s, cy + 90 * s)
    p_tl = (cx - 230 * s, cy - 170 * s)
    
    outer_poly = [p_top, p_tr, p_mr, p_bot, p_ml, p_tl]
    
    # Shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(outer_poly, fill=(0, 0, 0, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=25 * s))
    img = Image.alpha_composite(img, shadow)
    
    # Outer dark titanium fill
    m_draw.polygon(outer_poly, fill=(17, 24, 39, 255))
    
    # Vibrant dual-tone neon stroke
    m_draw.polygon(outer_poly, outline=(16, 185, 129, 255), width=int(7 * s)) # Emerald Green
    
    # Inner Floating Chevron "A" in Electric Cyan
    inner_poly = [
        (cx, cy - 210 * s),
        (cx + 155 * s, cy - 115 * s),
        (cx + 145 * s, cy + 60 * s),
        (cx, cy + 240 * s),
        (cx - 145 * s, cy + 60 * s),
        (cx - 155 * s, cy - 115 * s)
    ]
    m_draw.polygon(inner_poly, fill=(6, 182, 212, 255), outline=(248, 250, 252, 255), width=int(3 * s))
    
    # Cutout inside inner
    inner_core_cut = [
        (cx, cy - 120 * s),
        (cx + 80 * s, cy + 40 * s),
        (cx, cy + 150 * s),
        (cx - 80 * s, cy + 40 * s)
    ]
    m_draw.polygon(inner_core_cut, fill=(8, 12, 20, 255))
    
    # Central Platinum Diamond
    c_dia = [
        (cx, cy - 30 * s),
        (cx + 36 * s, cy + 15 * s),
        (cx, cy + 60 * s),
        (cx - 36 * s, cy + 15 * s)
    ]
    m_draw.polygon(c_dia, fill=(248, 250, 252, 255))
    
    img = Image.alpha_composite(img, mark)
    return img.resize((size, size), Image.Resampling.LANCZOS)

def create_option_c_swiss_titan(size=1024):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    pad = int(48 * s)
    corner_r = int(220 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    # Deep obsidian satin background
    bg = Image.new("RGBA", (W, H), (9, 13, 21, 255))
    
    # Elegant central blue illumination
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    for r in range(int(420 * s), 0, -8):
        a = int(36 * (1.0 - (r / (420 * s)) ** 1.6))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(14, 165, 233, a))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=20 * s))
    bg = Image.alpha_composite(bg, glow)
    
    # Hairline squircle border
    b_draw = ImageDraw.Draw(bg)
    b_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 220), width=int(2.5 * s))
    img.paste(bg, (0, 0), bg_mask)
    
    # World-Class Minimalist Monogram:
    # A continuous, interlocking geometric shield ribbon in pure Electric Cyan + Platinum
    mark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Drop shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    
    # Master geometric shield dimensions
    # Apex: (cx, cy - 300*s)
    # Shoulder: (cx + 235*s, cy - 180*s)
    # Waist: (cx + 225*s, cy + 70*s)
    # Tip: (cx, cy + 325*s)
    
    outer_pts = [
        (cx, cy - 300 * s),
        (cx + 235 * s, cy - 180 * s),
        (cx + 225 * s, cy + 70 * s),
        (cx, cy + 325 * s),
        (cx - 225 * s, cy + 70 * s),
        (cx - 235 * s, cy - 180 * s)
    ]
    s_draw.polygon(outer_pts, fill=(0, 0, 0, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=28 * s))
    img = Image.alpha_composite(img, shadow)
    
    # Solid dark titanium slab with 1px border
    m_draw.polygon(outer_pts, fill=(15, 23, 42, 255), outline=(51, 65, 85, 220), width=int(2 * s))
    
    # Inside the slab: Dual Precision Geometric Chvrons forming the "Aegis A"
    # Chevron 1 (Top Shield Crown / A-Head)
    c1 = [
        (cx, cy - 240 * s),
        (cx + 170 * s, cy - 145 * s),
        (cx + 140 * s, cy - 90 * s),
        (cx, cy - 170 * s),
        (cx - 140 * s, cy - 90 * s),
        (cx - 170 * s, cy - 145 * s)
    ]
    m_draw.polygon(c1, fill=(34, 211, 238, 255)) # Vibrant glowing cyan
    
    # Chevron 2 (Mid Wings)
    c2 = [
        (cx, cy - 120 * s),
        (cx + 170 * s, cy - 20 * s),
        (cx + 140 * s, cy + 40 * s),
        (cx, cy - 45 * s),
        (cx - 140 * s, cy + 40 * s),
        (cx - 170 * s, cy - 20 * s)
    ]
    m_draw.polygon(c2, fill=(6, 182, 212, 255)) # Deep cyan
    
    # Chevron 3 (Bottom Keel / Anchor)
    c3 = [
        (cx, cy + 10 * s),
        (cx + 160 * s, cy + 110 * s),
        (cx, cy + 270 * s),
        (cx - 160 * s, cy + 110 * s),
        (cx - 130 * s, cy + 70 * s),
        (cx, cy + 200 * s),
        (cx + 130 * s, cy + 70 * s)
    ]
    m_draw.polygon(c3, fill=(2, 132, 199, 255)) # Sapphire blue
    
    # Platinum Core Diamond (Focal Point)
    diamond = [
        (cx, cy - 70 * s),
        (cx + 38 * s, cy - 15 * s),
        (cx, cy + 40 * s),
        (cx - 38 * s, cy - 15 * s)
    ]
    m_draw.polygon(diamond, fill=(248, 250, 252, 255))
    
    img = Image.alpha_composite(img, mark)
    return img.resize((size, size), Image.Resampling.LANCZOS)

if __name__ == "__main__":
    opt_a = create_option_a_apex_prism(1024)
    opt_a.save("logo_option_a.png", "PNG")
    
    opt_b = create_option_b_cyber_monogram(1024)
    opt_b.save("logo_option_b.png", "PNG")
    
    opt_c = create_option_c_swiss_titan(1024)
    opt_c.save("logo_option_c.png", "PNG")
    
    print("Generated logo_option_a.png, logo_option_b.png, logo_option_c.png")
