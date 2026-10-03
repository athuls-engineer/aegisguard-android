import os, math
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
    for i in range(steps + 1):
        t = i / steps
        x = left_x + t * w
        arch = -math.sin(t * math.pi) * (h * 0.04)
        pts.append((x, top_y + arch))
        
    for i in range(1, steps // 2 + 1):
        t = i / (steps // 2)
        y = top_y + t * (waist_y - top_y)
        pts.append((right_x, y))
        
    p0 = (right_x, waist_y)
    p1 = (right_x, waist_y + h * 0.22)
    p2 = (cx + w * 0.18, bot_y)
    p3 = (cx, bot_y)
    for i in range(1, steps + 1):
        t = i / steps
        pts.append(bezier_point(p0, p1, p2, p3, t))
        
    p0_l = (cx, bot_y)
    p1_l = (cx - w * 0.18, bot_y)
    p2_l = (left_x, waist_y + h * 0.22)
    p3_l = (left_x, waist_y)
    for i in range(1, steps + 1):
        t = i / steps
        pts.append(bezier_point(p0_l, p1_l, p2_l, p3_l, t))
        
    for i in range(1, steps // 2):
        t = i / (steps // 2)
        y = waist_y - t * (waist_y - top_y)
        pts.append((left_x, y))
        
    return pts

def render_vanguard_bolt_logo(size=1024):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    pad = int(40 * s)
    corner_r = int(220 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    # Deep luxury dark obsidian canvas: #080C14 to #0E1626
    bg = Image.new("RGBA", (W, H), (8, 12, 20, 255))
    
    # Ambient cyan glow behind shield
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
    
    # Shield Drop Shadow
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
    
    # Bold, Prominent White Lightning Bolt (Optimized for small-scale readability)
    bolt_pts = [
        (cx + 35 * s, cy - 175 * s),  # Top right apex
        (cx - 115 * s, cy + 10 * s),  # Mid left outer
        (cx - 20 * s, cy + 10 * s),   # Mid left inner
        (cx - 50 * s, cy + 185 * s),  # Bottom point
        (cx + 115 * s, cy - 10 * s),  # Mid right outer
        (cx + 20 * s, cy - 10 * s)    # Mid right inner
    ]
    
    b_shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(b_shadow).polygon(bolt_pts, fill=(0, 0, 0, 100))
    b_shadow = b_shadow.filter(ImageFilter.GaussianBlur(radius=8 * s))
    img = Image.alpha_composite(img, b_shadow)
    
    bolt = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(bolt).polygon(bolt_pts, fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, bolt)
    
    return img.resize((size, size), Image.Resampling.LANCZOS)

def export_all():
    base_res = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    print("Rendering 1024x1024 Master Vanguard Bolt...")
    master = render_vanguard_bolt_logo(1024)
    
    # Export to mipmap directories as direct PNGs
    for folder, dim in densities.items():
        folder_path = os.path.join(base_res, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Standard Launcher Icon
        icon_resized = master.resize((dim, dim), Image.Resampling.LANCZOS)
        icon_resized.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
        icon_resized.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
        print(f"Exported {folder}: {dim}x{dim}")
        
    # Also save as header logo drawable
    drawable_path = os.path.join(base_res, "drawable")
    header_logo = master.resize((128, 128), Image.Resampling.LANCZOS)
    header_logo.save(os.path.join(drawable_path, "ic_header_logo.png"), "PNG")
    print("Exported drawable/ic_header_logo.png")
    
    # Save to Downloads and Brain
    dl_path = r"C:\Users\athul_nuy2ni9\Downloads\AegisGuard-Logo-Vanguard.png"
    master.save(dl_path, "PNG")
    master.save(r"C:\Users\athul_nuy2ni9\Downloads\aegisguard_brand_logo.png", "PNG")
    
    brain_path = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3\aegisguard_brand_logo.png"
    master.save(brain_path, "PNG")
    master.save(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3\AegisGuard-Logo-Vanguard.png", "PNG")
    
    print("All Vanguard Bolt brand assets exported successfully!")

if __name__ == "__main__":
    export_all()
