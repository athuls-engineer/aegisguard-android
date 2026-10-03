import math
from PIL import Image, ImageDraw, ImageFilter

def bezier_point(p0, p1, p2, p3, t):
    u = 1 - t
    tt = t * t
    uu = u * u
    uuu = uu * u
    ttt = tt * t
    x = uuu * p0[0] + 3 * uu * t * p1[0] + 3 * u * tt * p2[0] + ttt * p3[0]
    y = uuu * p0[1] + 3 * uu * t * p1[1] + 3 * u * tt * p2[1] + ttt * p3[1]
    return (x, y)

def get_modern_shield_points(cx, cy, w, h, steps=40):
    top_y = cy - h * 0.48
    waist_y = cy + h * 0.06
    bot_y = cy + h * 0.50
    left_x = cx - w * 0.50
    right_x = cx + w * 0.50
    
    pts = []
    # 1. Top edge: gentle curved crown
    for i in range(steps + 1):
        t = i / steps
        x = left_x + t * w
        arch = -math.sin(t * math.pi) * (h * 0.04)
        pts.append((x, top_y + arch))
        
    # 2. Right vertical waist edge
    for i in range(1, steps // 2 + 1):
        t = i / (steps // 2)
        y = top_y + t * (waist_y - top_y)
        pts.append((right_x, y))
        
    # 3. Right bottom curve to tip
    p0 = (right_x, waist_y)
    p1 = (right_x, waist_y + h * 0.22)
    p2 = (cx + w * 0.18, bot_y)
    p3 = (cx, bot_y)
    for i in range(1, steps + 1):
        t = i / steps
        pts.append(bezier_point(p0, p1, p2, p3, t))
        
    # 4. Left bottom curve from tip to waist
    p0_l = (cx, bot_y)
    p1_l = (cx - w * 0.18, bot_y)
    p2_l = (left_x, waist_y + h * 0.22)
    p3_l = (left_x, waist_y)
    for i in range(1, steps + 1):
        t = i / steps
        pts.append(bezier_point(p0_l, p1_l, p2_l, p3_l, t))
        
    # 5. Left waist up to top left
    for i in range(1, steps // 2):
        t = i / (steps // 2)
        y = waist_y - t * (waist_y - top_y)
        pts.append((left_x, y))
        
    return pts

def render_concept_vault(size=1024):
    # Concept A: The Aegis Vault (Cyan/Sapphire Shield + Minimalist White Keyhole)
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    pad = int(40 * s)
    corner_r = int(220 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    bg = Image.new("RGBA", (W, H), (8, 12, 20, 255))
    
    # Deep cyan ambient backlight
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    for r in range(int(460 * s), 0, -8):
        a = int(35 * (1.0 - (r / (460 * s)) ** 1.6))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(6, 182, 212, a))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=25 * s))
    bg = Image.alpha_composite(bg, glow)
    
    ImageDraw.Draw(bg).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 220), width=int(2.5 * s))
    img.paste(bg, (0, 0), bg_mask)
    
    w = 560 * s
    h = 680 * s
    shield_pts = get_modern_shield_points(cx, cy - 10 * s, w, h)
    
    # Shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(shield_pts, fill=(0, 0, 0, 200))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=30 * s))
    img = Image.alpha_composite(img, shadow)
    
    # Shield Body: Left Sapphire, Right Electric Cyan
    left_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(left_mask).rectangle([0, 0, cx, H], fill=255)
    
    l_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(l_img).polygon(shield_pts, fill=(2, 132, 199, 255)) # Sapphire #0284C7
    
    r_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(r_img).polygon(shield_pts, fill=(6, 182, 212, 255)) # Electric Cyan #06B6D4
    
    shield_img = Image.composite(l_img, r_img, left_mask)
    
    # Top highlight rim
    ImageDraw.Draw(shield_img).polygon(shield_pts, outline=(255, 255, 255, 120), width=int(3 * s))
    ImageDraw.Draw(shield_img).line([(cx, cy - h * 0.48), (cx, cy + h * 0.50 - 10 * s)], fill=(255, 255, 255, 70), width=int(1.5 * s))
    img = Image.alpha_composite(img, shield_img)
    
    # Central Minimalist Pure White Vault Keyhole
    hole = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    h_draw = ImageDraw.Draw(hole)
    
    kh_cy = cy - 25 * s
    kh_r = 85 * s
    h_draw.ellipse([cx - kh_r, kh_cy - kh_r, cx + kh_r, kh_cy + kh_r], fill=(255, 255, 255, 255))
    
    stem_top_w = 48 * s
    stem_bot_w = 70 * s
    stem_h = 145 * s
    stem_pts = [
        (cx - stem_top_w, kh_cy),
        (cx + stem_top_w, kh_cy),
        (cx + stem_bot_w, kh_cy + stem_h),
        (cx - stem_bot_w, kh_cy + stem_h)
    ]
    h_draw.polygon(stem_pts, fill=(255, 255, 255, 255))
    h_draw.ellipse([cx - stem_bot_w, kh_cy + stem_h - 25 * s, cx + stem_bot_w, kh_cy + stem_h + 25 * s], fill=(255, 255, 255, 255))
    
    # Shadow under keyhole
    h_shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(h_shadow).ellipse([cx - kh_r, kh_cy - kh_r, cx + kh_r, kh_cy + kh_r], fill=(0, 0, 0, 80))
    ImageDraw.Draw(h_shadow).polygon(stem_pts, fill=(0, 0, 0, 80))
    ImageDraw.Draw(h_shadow).ellipse([cx - stem_bot_w, kh_cy + stem_h - 25 * s, cx + stem_bot_w, kh_cy + stem_h + 25 * s], fill=(0, 0, 0, 80))
    h_shadow = h_shadow.filter(ImageFilter.GaussianBlur(radius=8 * s))
    img = Image.alpha_composite(img, h_shadow)
    
    img = Image.alpha_composite(img, hole)
    return img.resize((size, size), Image.Resampling.LANCZOS)

def render_concept_bolt(size=1024):
    # Concept B: The Aegis Vanguard Bolt (Electric Cyan/Sapphire Shield + Razor White Lightning Spear)
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    pad = int(40 * s)
    corner_r = int(220 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    bg = Image.new("RGBA", (W, H), (8, 12, 20, 255))
    
    # Ambient cyan glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    for r in range(int(460 * s), 0, -8):
        a = int(35 * (1.0 - (r / (460 * s)) ** 1.6))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(6, 182, 212, a))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=25 * s))
    bg = Image.alpha_composite(bg, glow)
    
    ImageDraw.Draw(bg).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 220), width=int(2.5 * s))
    img.paste(bg, (0, 0), bg_mask)
    
    w = 560 * s
    h = 680 * s
    shield_pts = get_modern_shield_points(cx, cy - 10 * s, w, h)
    
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(shield_pts, fill=(0, 0, 0, 200))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=30 * s))
    img = Image.alpha_composite(img, shadow)
    
    # Shield Body
    left_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(left_mask).rectangle([0, 0, cx, H], fill=255)
    
    l_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(l_img).polygon(shield_pts, fill=(2, 132, 199, 255)) # Sapphire #0284C7
    
    r_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(r_img).polygon(shield_pts, fill=(6, 182, 212, 255)) # Electric Cyan #06B6D4
    
    shield_img = Image.composite(l_img, r_img, left_mask)
    ImageDraw.Draw(shield_img).polygon(shield_pts, outline=(255, 255, 255, 120), width=int(3 * s))
    ImageDraw.Draw(shield_img).line([(cx, cy - h * 0.48), (cx, cy + h * 0.50 - 10 * s)], fill=(255, 255, 255, 70), width=int(1.5 * s))
    img = Image.alpha_composite(img, shield_img)
    
    # Central Bold Lightning Bolt (Zero-Latency Speed + Protection)
    bolt_pts = [
        (cx + 25 * s, cy - 165 * s),  # Top right apex
        (cx - 105 * s, cy + 10 * s),  # Mid left outer
        (cx - 15 * s, cy + 10 * s),   # Mid left inner
        (cx - 45 * s, cy + 175 * s),  # Bottom left point
        (cx + 105 * s, cy - 5 * s),   # Mid right outer
        (cx + 15 * s, cy - 5 * s)     # Mid right inner
    ]
    
    # Bolt shadow
    b_shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(b_shadow).polygon(bolt_pts, fill=(0, 0, 0, 90))
    b_shadow = b_shadow.filter(ImageFilter.GaussianBlur(radius=8 * s))
    img = Image.alpha_composite(img, b_shadow)
    
    bolt = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(bolt).polygon(bolt_pts, fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, bolt)
    
    return img.resize((size, size), Image.Resampling.LANCZOS)

if __name__ == "__main__":
    vault = render_concept_vault(1024)
    vault.save("concept_vault.png", "PNG")
    
    bolt = render_concept_bolt(1024)
    bolt.save("concept_bolt.png", "PNG")
    
    print("Generated concept_vault.png and concept_bolt.png")
