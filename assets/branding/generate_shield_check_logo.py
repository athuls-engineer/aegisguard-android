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

def generate_classic_shield_points(cx, cy, w, h, steps=40):
    # Top points: gentle curved top
    top_y = cy - h * 0.48
    waist_y = cy + h * 0.05
    bot_y = cy + h * 0.50
    left_x = cx - w * 0.50
    right_x = cx + w * 0.50
    
    pts = []
    
    # 1. Top edge: gentle arch from left to right
    for i in range(steps + 1):
        t = i / steps
        x = left_x + t * w
        # subtle curve upwards in center
        arch = -math.sin(t * math.pi) * (h * 0.04)
        pts.append((x, top_y + arch))
        
    # 2. Right vertical/waist edge
    for i in range(1, steps // 2 + 1):
        t = i / (steps // 2)
        y = top_y + t * (waist_y - top_y)
        pts.append((right_x, y))
        
    # 3. Right bottom curve to tip using cubic bezier
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

def create_shield_logo(size=1024, style="emerald_cyan", is_foreground_only=False):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    if not is_foreground_only:
        # Full Squircle Canvas
        pad = int(40 * s)
        corner_r = int(220 * s)
        
        bg_mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
        
        # Deep luxury dark obsidian canvas: #080C14 to #0E1626
        bg = Image.new("RGBA", (W, H), (9, 13, 21, 255))
        
        # Soft ambient glow behind shield
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        g_draw = ImageDraw.Draw(glow)
        glow_color = (16, 185, 129, 32) if "emerald" in style else (6, 182, 212, 32)
        for r in range(int(460 * s), 0, -8):
            a = int(35 * (1.0 - (r / (460 * s)) ** 1.6))
            fill_c = (glow_color[0], glow_color[1], glow_color[2], a)
            g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill_c)
        glow = glow.filter(ImageFilter.GaussianBlur(radius=25 * s))
        bg = Image.alpha_composite(bg, glow)
        
        # Subtle perimeter rim
        b_draw = ImageDraw.Draw(bg)
        b_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 220), width=int(2.5 * s))
        img.paste(bg, (0, 0), bg_mask)
    
    # Generate the pristine shield points
    w = 560 * s
    h = 680 * s
    shield_pts = generate_classic_shield_points(cx, cy - 10 * s, w, h)
    
    # 1. Elegant Deep Drop Shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(shield_pts, fill=(0, 0, 0, 200))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=30 * s))
    img = Image.alpha_composite(img, shadow)
    
    # 2. Shield Body Rendering
    shield_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shield_img)
    
    if style == "emerald":
        # Pure AdGuard Style: Rich Emerald Gradient (#10B981 to #059669)
        # Left half: Rich Emerald
        left_mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(left_mask).rectangle([0, 0, cx, H], fill=255)
        
        l_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(l_img).polygon(shield_pts, fill=(5, 150, 105, 255)) # Darker emerald #059669
        
        r_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(r_img).polygon(shield_pts, fill=(16, 185, 129, 255)) # Radiant emerald #10B981
        
        shield_img = Image.composite(l_img, r_img, left_mask)
        
    elif style == "cyan":
        # Pure Aegis Cyan / Sapphire Style (#06B6D4 to #0284C7)
        left_mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(left_mask).rectangle([0, 0, cx, H], fill=255)
        
        l_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(l_img).polygon(shield_pts, fill=(2, 132, 199, 255)) # Sapphire #0284C7
        
        r_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(r_img).polygon(shield_pts, fill=(6, 182, 212, 255)) # Electric Cyan #06B6D4
        
        shield_img = Image.composite(l_img, r_img, left_mask)
        
    else: # "emerald_cyan" (Masterpiece: Dual Defense Synthesis)
        # Emerald on left (Privacy/Ad-blocking), Electric Cyan on right (Turbo DNS Speed)
        left_mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(left_mask).rectangle([0, 0, cx, H], fill=255)
        
        l_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(l_img).polygon(shield_pts, fill=(16, 185, 129, 255)) # Emerald Green
        
        r_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(r_img).polygon(shield_pts, fill=(6, 182, 212, 255)) # Cyan
        
        shield_img = Image.composite(l_img, r_img, left_mask)
        
    # Subtle inner light bevel highlight around top edge
    s_draw = ImageDraw.Draw(shield_img)
    s_draw.polygon(shield_pts, outline=(255, 255, 255, 110), width=int(3 * s))
    
    # Center vertical subtle line
    s_draw.line([(cx, cy - h * 0.48), (cx, cy + h * 0.50 - 10 * s)], fill=(255, 255, 255, 80), width=int(1.5 * s))
    
    img = Image.alpha_composite(img, shield_img)
    
    # 3. Bold, World-Class White Checkmark (Universally Recognized Protection Symbol)
    # Perfectly centered optical coordinates:
    chk_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(chk_layer)
    
    # Checkmark points:
    # Short tail (left): (cx - 130*s, cy + 10*s)
    # Vertex (bottom): (cx - 30*s, cy + 120*s)
    # Long arm (top right): (cx + 145*s, cy - 90*s)
    p1 = (cx - 130 * s, cy + 5 * s)
    p2 = (cx - 30 * s, cy + 115 * s)
    p3 = (cx + 145 * s, cy - 85 * s)
    
    # Stroke thickness: bold and authoritative
    chk_width = int(58 * s)
    
    # Checkmark soft drop shadow for physical presence
    chk_shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cs_draw = ImageDraw.Draw(chk_shadow)
    cs_draw.line([p1, p2], fill=(0, 0, 0, 90), width=chk_width)
    cs_draw.line([p2, p3], fill=(0, 0, 0, 90), width=chk_width)
    for pt in [p1, p2, p3]:
        cs_draw.ellipse([pt[0] - chk_width//2, pt[1] - chk_width//2, pt[0] + chk_width//2, pt[1] + chk_width//2], fill=(0, 0, 0, 90))
    chk_shadow = chk_shadow.filter(ImageFilter.GaussianBlur(radius=8 * s))
    img = Image.alpha_composite(img, chk_shadow)
    
    # Pure White Checkmark with Rounded Caps
    c_draw.line([p1, p2], fill=(255, 255, 255, 255), width=chk_width)
    c_draw.line([p2, p3], fill=(255, 255, 255, 255), width=chk_width)
    for pt in [p1, p2, p3]:
        c_draw.ellipse([pt[0] - chk_width//2, pt[1] - chk_width//2, pt[0] + chk_width//2, pt[1] + chk_width//2], fill=(255, 255, 255, 255))
        
    img = Image.alpha_composite(img, chk_layer)
    
    final = img.resize((size, size), Image.Resampling.LANCZOS)
    return final

if __name__ == "__main__":
    emerald = create_shield_logo(1024, style="emerald")
    emerald.save("shield_check_emerald.png", "PNG")
    
    cyan = create_shield_logo(1024, style="cyan")
    cyan.save("shield_check_cyan.png", "PNG")
    
    dual = create_shield_logo(1024, style="emerald_cyan")
    dual.save("shield_check_dual.png", "PNG")
    
    print("Generated shield_check_emerald.png, shield_check_cyan.png, shield_check_dual.png")
