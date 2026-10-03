import math
from PIL import Image, ImageDraw, ImageFilter

def draw_smooth_shield(draw, cx, cy, w, h, fill=None, outline=None, width=1):
    # Generates a smooth, curved modern shield polygon
    pts = []
    # Top edge: subtle arch
    steps = 16
    for i in range(steps + 1):
        t = i / steps
        x = (cx - w/2) + t * w
        # subtle curve up in middle
        arch = -math.sin(t * math.pi) * (h * 0.05)
        y = (cy - h/2) + arch
        pts.append((x, y))
    
    # Right side down to waist
    steps_side = 16
    for i in range(1, steps_side + 1):
        t = i / steps_side
        # curves outwards slightly then in towards bottom tip
        x = (cx + w/2) - (t ** 2) * (w * 0.5)
        y = (cy - h/2) + (h * 0.45) + t * (h * 0.55)
        pts.append((x, y))
        
    # Left side up from bottom tip
    for i in range(1, steps_side):
        t = i / steps_side
        inv_t = 1.0 - t
        x = cx - (w * 0.5) * (inv_t ** 0.5)
        y = (cy + h/2) - t * (h * 0.55)
        # We need the inverse to go upwards
    
    # Let's do explicit parametric bezier for a flawless Apple/Swiss curved shield:
    return

def make_concept_1_vault_shield(size=512):
    # Concept 1: "The Vault Shield" (Minimalist Swiss Security - like 1Password/Bitwarden)
    # Smooth deep navy-black squircle, glowing cyan rounded shield, white keyhole
    scale = 2
    S = size * scale
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    pad = int(32 * scale)
    r = int(110 * scale)
    
    # Background: Smooth dark navy
    bg = Image.new("RGBA", (S, S), (11, 15, 25, 255))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, fill=255)
    
    # Soft blue/cyan ambient backlight
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    cx, cy = S // 2, S // 2
    for rad in range(int(240 * scale), 0, -4):
        alpha = int(40 * (1.0 - rad / (240 * scale)))
        gdraw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(14, 165, 233, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(16 * scale))
    bg = Image.alpha_composite(bg, glow)
    
    # Subtle border
    ImageDraw.Draw(bg).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, outline=(30, 41, 59, 200), width=int(2 * scale))
    img.paste(bg, (0, 0), mask)
    
    # Smooth curved shield
    # Parametric curve points
    sh_w = 170 * scale
    sh_h = 220 * scale
    top_y = cy - 105 * scale
    bot_y = cy + 115 * scale
    
    # Sample 100 points along a smooth shield silhouette
    shield_pts = []
    # top curve: from left to right
    for i in range(30):
        t = i / 29.0
        x = (cx - sh_w) + t * (sh_w * 2)
        y = top_y - math.sin(t * math.pi) * (18 * scale)
        shield_pts.append((x, y))
    # right curve down to tip
    for i in range(1, 40):
        t = i / 39.0
        # cubic bezier curve to tip
        x = (cx + sh_w) * (1 - t) + cx * t - math.sin(t * math.pi) * (15 * scale)
        y = top_y + t * (bot_y - top_y)
        shield_pts.append((x, y))
    # left curve from tip back to top left
    for i in range(1, 40):
        t = i / 39.0
        x = cx * (1 - t) + (cx - sh_w) * t + math.sin(t * math.pi) * (15 * scale)
        y = bot_y - t * (bot_y - top_y)
        shield_pts.append((x, y))
        
    # Drop shadow under shield
    sh_shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh_shadow).polygon(shield_pts, fill=(0, 0, 0, 180))
    sh_shadow = sh_shadow.filter(ImageFilter.GaussianBlur(18 * scale))
    img = Image.alpha_composite(img, sh_shadow)
    
    # Draw Shield Solid (Electric Cyan Gradient)
    shield_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shield_layer)
    s_draw.polygon(shield_pts, fill=(6, 182, 212, 255)) # Pure clean cyan
    
    # Left half subtle shading for gentle depth (not clunky polygons)
    left_mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(left_mask).rectangle([0, 0, cx, S], fill=255)
    s_draw.polygon(shield_pts, fill=(2, 132, 199, 255)) # Sapphire #0284C7
    
    # Right half clean cyan
    right_mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(right_mask).rectangle([cx, 0, S, S], fill=255)
    
    # Combine split
    left_img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(left_img).polygon(shield_pts, fill=(2, 132, 199, 255))
    right_img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(right_img).polygon(shield_pts, fill=(34, 211, 238, 255))
    
    shield_combined = Image.composite(left_img, right_img, left_mask)
    
    # Clean, minimalist Keyhole / Cryptographic Core in crisp pure white
    # Circle top + tapered trapezoid bottom
    hole_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    h_draw = ImageDraw.Draw(hole_layer)
    
    kh_y = cy - 10 * scale
    kh_r = 32 * scale
    h_draw.ellipse([cx - kh_r, kh_y - kh_r, cx + kh_r, kh_y + kh_r], fill=(255, 255, 255, 255))
    
    # Keyhole stem
    stem_pts = [
        (cx - 18 * scale, kh_y),
        (cx + 18 * scale, kh_y),
        (cx + 26 * scale, kh_y + 54 * scale),
        (cx - 26 * scale, kh_y + 54 * scale)
    ]
    h_draw.polygon(stem_pts, fill=(255, 255, 255, 255))
    h_draw.ellipse([cx - 26 * scale, kh_y + 40 * scale, cx + 26 * scale, kh_y + 68 * scale], fill=(255, 255, 255, 255))
    
    # Soft inner shadow in keyhole
    shield_combined.paste(hole_layer, (0, 0), hole_layer)
    img = Image.alpha_composite(img, shield_combined)
    
    return img.resize((size, size), Image.Resampling.LANCZOS)

def make_concept_2_adguard_elite(size=512):
    # Concept 2: "The Guardian Check" (Clean, Vibrant, Friendly Security - like AdGuard/NextDNS)
    # Vibrant Emerald/Mint + Cyan shield with a bold, clean white rounded checkmark
    scale = 2
    S = size * scale
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    
    pad = int(32 * scale)
    r = int(110 * scale)
    
    bg = Image.new("RGBA", (S, S), (10, 15, 24, 255))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, fill=255)
    
    # Soft emerald glow
    cx, cy = S // 2, S // 2
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for rad in range(int(240 * scale), 0, -4):
        alpha = int(45 * (1.0 - rad / (240 * scale)))
        gdraw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(16, 185, 129, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(16 * scale))
    bg = Image.alpha_composite(bg, glow)
    
    ImageDraw.Draw(bg).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, outline=(30, 41, 59, 200), width=int(2 * scale))
    img.paste(bg, (0, 0), mask)
    
    # Smooth Shield Outline or Solid
    sh_w = 165 * scale
    top_y = cy - 105 * scale
    bot_y = cy + 115 * scale
    
    shield_pts = []
    for i in range(30):
        t = i / 29.0
        x = (cx - sh_w) + t * (sh_w * 2)
        y = top_y - math.sin(t * math.pi) * (18 * scale)
        shield_pts.append((x, y))
    for i in range(1, 40):
        t = i / 39.0
        x = (cx + sh_w) * (1 - t) + cx * t - math.sin(t * math.pi) * (15 * scale)
        y = top_y + t * (bot_y - top_y)
        shield_pts.append((x, y))
    for i in range(1, 40):
        t = i / 39.0
        x = cx * (1 - t) + (cx - sh_w) * t + math.sin(t * math.pi) * (15 * scale)
        y = bot_y - t * (bot_y - top_y)
        shield_pts.append((x, y))
        
    sh_shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh_shadow).polygon(shield_pts, fill=(0, 0, 0, 180))
    sh_shadow = sh_shadow.filter(ImageFilter.GaussianBlur(18 * scale))
    img = Image.alpha_composite(img, sh_shadow)
    
    # Solid Emerald Green Gradient Shield
    shield_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shield_layer)
    s_draw.polygon(shield_pts, fill=(16, 185, 129, 255)) # Vibrant Emerald #10B981
    
    # Right half slightly lighter mint for subtle dimension
    right_mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(right_mask).rectangle([cx, 0, S, S], fill=255)
    right_img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(right_img).polygon(shield_pts, fill=(52, 211, 153, 255)) # Mint #34D399
    shield_layer = Image.composite(right_img, shield_layer, right_mask)
    
    # Bold, clean, rounded checkmark in pure white
    # Points for checkmark: left, vertex, right
    chk_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(chk_layer)
    
    p1 = (cx - 65 * scale, cy + 5 * scale)
    p2 = (cx - 15 * scale, cy + 55 * scale)
    p3 = (cx + 70 * scale, cy - 40 * scale)
    
    # Thick rounded check stroke
    w_chk = int(28 * scale)
    c_draw.line([p1, p2], fill=(255, 255, 255, 255), width=w_chk)
    c_draw.line([p2, p3], fill=(255, 255, 255, 255), width=w_chk)
    # Round caps
    for pt in [p1, p2, p3]:
        c_draw.ellipse([pt[0] - w_chk//2, pt[1] - w_chk//2, pt[0] + w_chk//2, pt[1] + w_chk//2], fill=(255, 255, 255, 255))
        
    shield_layer.paste(chk_layer, (0, 0), chk_layer)
    img = Image.alpha_composite(img, shield_layer)
    
    return img.resize((size, size), Image.Resampling.LANCZOS)

def make_concept_3_minimal_monogram(size=512):
    # Concept 3: "The Fluid Aegis" (Apple/Proton Style - Minimalist Fluid Shield Outline)
    # A continuous ultra-sleek cyan-blue neon loop forming a minimalist shield on deep obsidian
    scale = 2
    S = size * scale
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    
    pad = int(32 * scale)
    r = int(110 * scale)
    
    bg = Image.new("RGBA", (S, S), (10, 14, 22, 255))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, fill=255)
    
    cx, cy = S // 2, S // 2
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for rad in range(int(240 * scale), 0, -4):
        alpha = int(35 * (1.0 - rad / (240 * scale)))
        gdraw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(6, 182, 212, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(16 * scale))
    bg = Image.alpha_composite(bg, glow)
    
    ImageDraw.Draw(bg).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, outline=(30, 41, 59, 200), width=int(2 * scale))
    img.paste(bg, (0, 0), mask)
    
    # Smooth Shield Outline (Thick 20px tube, hollow center with lightning bolt or dot)
    sh_w = 160 * scale
    top_y = cy - 105 * scale
    bot_y = cy + 115 * scale
    
    shield_pts = []
    for i in range(30):
        t = i / 29.0
        x = (cx - sh_w) + t * (sh_w * 2)
        y = top_y - math.sin(t * math.pi) * (18 * scale)
        shield_pts.append((x, y))
    for i in range(1, 40):
        t = i / 39.0
        x = (cx + sh_w) * (1 - t) + cx * t - math.sin(t * math.pi) * (15 * scale)
        y = top_y + t * (bot_y - top_y)
        shield_pts.append((x, y))
    for i in range(1, 40):
        t = i / 39.0
        x = cx * (1 - t) + (cx - sh_w) * t + math.sin(t * math.pi) * (15 * scale)
        y = bot_y - t * (bot_y - top_y)
        shield_pts.append((x, y))
        
    line_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    l_draw = ImageDraw.Draw(line_layer)
    
    # Glow outline
    l_draw.polygon(shield_pts, outline=(34, 211, 238, 255), width=int(24 * scale))
    # Soft outline glow
    glow_line = line_layer.filter(ImageFilter.GaussianBlur(8 * scale))
    img = Image.alpha_composite(img, glow_line)
    
    # Solid clean outline
    core_line = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(core_line).polygon(shield_pts, outline=(34, 211, 238, 255), width=int(20 * scale))
    img = Image.alpha_composite(img, core_line)
    
    # Inside: Pure clean Lightning Bolt (Speed + Protection) in brilliant white
    bolt = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(bolt)
    
    bolt_pts = [
        (cx + 8 * scale, cy - 65 * scale),
        (cx - 40 * scale, cy + 5 * scale),
        (cx - 2 * scale, cy + 5 * scale),
        (cx - 15 * scale, cy + 68 * scale),
        (cx + 42 * scale, cy - 5 * scale),
        (cx + 5 * scale, cy - 5 * scale)
    ]
    b_draw.polygon(bolt_pts, fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, bolt)
    
    return img.resize((size, size), Image.Resampling.LANCZOS)

def make_concept_4_pure_minimal_a(size=512):
    # Concept 4: "The Pure Minimal A-Shield" (Tesla/Brave clean typography silhouette)
    # A single, bold white & cyan continuous stylized A-Shield silhouette on dark navy
    scale = 2
    S = size * scale
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    
    pad = int(32 * scale)
    r = int(110 * scale)
    
    bg = Image.new("RGBA", (S, S), (9, 13, 20, 255))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, fill=255)
    
    cx, cy = S // 2, S // 2
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for rad in range(int(240 * scale), 0, -4):
        alpha = int(35 * (1.0 - rad / (240 * scale)))
        gdraw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(14, 165, 233, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(16 * scale))
    bg = Image.alpha_composite(bg, glow)
    
    ImageDraw.Draw(bg).rounded_rectangle([pad, pad, S-pad, S-pad], radius=r, outline=(30, 41, 59, 200), width=int(2 * scale))
    img.paste(bg, (0, 0), mask)
    
    # Bold, clean minimalist symbol: An inverted apex chevron + floating diamond
    mark = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Outer Bold Shield Ribbon (24px thickness)
    # Smooth minimalist modern shield
    w = 160 * scale
    h = 210 * scale
    
    # Left chevron leg
    p_top = (cx, cy - 100 * scale)
    p_tl = (cx - 150 * scale, cy - 35 * scale)
    p_bl = (cx - 130 * scale, cy + 45 * scale)
    p_bot = (cx, cy + 125 * scale)
    p_br = (cx + 130 * scale, cy + 45 * scale)
    p_tr = (cx + 150 * scale, cy - 35 * scale)
    
    leg_w = int(24 * scale)
    # Smooth lines
    m_draw.line([p_top, p_tl, p_bl, p_bot], fill=(2, 132, 199, 255), width=leg_w, joint="curve")
    m_draw.line([p_top, p_tr, p_br, p_bot], fill=(6, 182, 212, 255), width=leg_w, joint="curve")
    # Top and bottom joins
    m_draw.ellipse([p_top[0]-leg_w//2, p_top[1]-leg_w//2, p_top[0]+leg_w//2, p_top[1]+leg_w//2], fill=(34, 211, 238, 255))
    m_draw.ellipse([p_bot[0]-leg_w//2, p_bot[1]-leg_w//2, p_bot[0]+leg_w//2, p_bot[1]+leg_w//2], fill=(6, 182, 212, 255))
    
    # In center: Bold clean solid Platinum Diamond
    dia_r = 44 * scale
    d_pts = [
        (cx, cy - 10 * scale - dia_r),
        (cx + dia_r, cy - 10 * scale),
        (cx, cy - 10 * scale + dia_r),
        (cx - dia_r, cy - 10 * scale)
    ]
    m_draw.polygon(d_pts, fill=(255, 255, 255, 255))
    
    img = Image.alpha_composite(img, mark)
    return img.resize((size, size), Image.Resampling.LANCZOS)

if __name__ == "__main__":
    c1 = make_concept_1_vault_shield(512)
    c1.save("concept_1_vault.png", "PNG")
    
    c2 = make_concept_2_adguard_elite(512)
    c2.save("concept_2_check.png", "PNG")
    
    c3 = make_concept_3_minimal_monogram(512)
    c3.save("concept_3_lightning.png", "PNG")
    
    c4 = make_concept_4_pure_minimal_a(512)
    c4.save("concept_4_diamond.png", "PNG")
    
    # Create single side-by-side showcase image (1024 x 1024 grid)
    grid = Image.new("RGBA", (1024, 1024), (10, 14, 22, 255))
    grid.paste(c1, (0, 0))
    grid.paste(c2, (512, 0))
    grid.paste(c3, (0, 512))
    grid.paste(c4, (512, 512))
    grid.save("logo_options_showcase.png", "PNG")
    
    # Copy to downloads and brain
    grid.save(r"C:\Users\athul_nuy2ni9\Downloads\logo_options_showcase.png", "PNG")
    grid.save(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3\logo_options_showcase.png", "PNG")
    
    print("All 4 clean concepts generated successfully!")
