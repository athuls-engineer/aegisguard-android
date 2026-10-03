import os
from PIL import Image, ImageDraw, ImageFilter

def render_master_aegis(size=1024, is_foreground_only=False):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    if not is_foreground_only:
        # Full Squircle Icon with satin finish
        pad = int(40 * s)
        corner_r = int(220 * s)
        
        bg_mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
        
        # Deep luxury obsidian gradient
        bg = Image.new("RGBA", (W, H), (8, 12, 20, 255))
        
        # Soft atmospheric cyan/blue radial bloom
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        g_draw = ImageDraw.Draw(glow)
        for r in range(int(480 * s), 0, -8):
            a = int(34 * (1.0 - (r / (480 * s)) ** 1.7))
            g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(6, 182, 212, a))
        glow = glow.filter(ImageFilter.GaussianBlur(radius=26 * s))
        bg = Image.alpha_composite(bg, glow)
        
        # Satin border rim
        b_draw = ImageDraw.Draw(bg)
        b_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 240), width=int(2.5 * s))
        img.paste(bg, (0, 0), bg_mask)
    
    # Hero Shield Dimensions (Tuned for ~70% optical height)
    # Apex top
    top_y = cy - 325 * s
    # Bottom point
    bot_y = cy + 355 * s
    # Shoulder width
    sh_w = 250 * s
    sh_y = cy - 190 * s
    # Mid waist
    w_w = 240 * s
    w_y = cy + 95 * s
    
    shield_pts = [
        (cx, top_y),
        (cx + sh_w, sh_y),
        (cx + w_w, w_y),
        (cx, bot_y),
        (cx - w_w, w_y),
        (cx - sh_w, sh_y)
    ]
    
    # Drop shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(shield_pts, fill=(0, 0, 0, 220))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=34 * s))
    img = Image.alpha_composite(img, shadow)
    
    mark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Titanium Shield Body - Left Facet (Dark Charcoal)
    left_body = [
        (cx, top_y),
        (cx - sh_w, sh_y),
        (cx - w_w, w_y),
        (cx, bot_y),
        (cx, top_y)
    ]
    m_draw.polygon(left_body, fill=(15, 23, 40, 255)) # Deep Titanium Slate
    
    # Titanium Shield Body - Right Facet (Gunmetal Metallic)
    right_body = [
        (cx, top_y),
        (cx + sh_w, sh_y),
        (cx + w_w, w_y),
        (cx, bot_y),
        (cx, top_y)
    ]
    m_draw.polygon(right_body, fill=(24, 34, 52, 255)) # Metallic Gunmetal
    
    # Precision Chamfer Rim
    m_draw.polygon(shield_pts, outline=(51, 65, 85, 240), width=int(3 * s))
    
    # Specular Edge on Top Ridges
    m_draw.line([(cx, top_y), (cx - sh_w, sh_y)], fill=(148, 163, 184, 180), width=int(2 * s))
    m_draw.line([(cx, top_y), (cx + sh_w, sh_y)], fill=(248, 250, 252, 240), width=int(2.5 * s))
    
    # Vertical Titanium Spine
    m_draw.line([(cx, top_y), (cx, bot_y)], fill=(71, 85, 105, 170), width=int(2 * s))
    
    # 3. The Hero "Apex Vanguard A" - Electric Cyan & Sapphire Jewel
    a_top = cy - 215 * s
    a_mid_cross = cy - 90 * s
    a_left_tip = (cx - 160 * s, cy + 135 * s)
    a_left_inner = (cx - 90 * s, cy + 135 * s)
    a_right_tip = (cx + 160 * s, cy + 135 * s)
    a_right_inner = (cx + 90 * s, cy + 135 * s)
    
    # Left A-Wing (Sapphire Blue)
    left_a_wing = [
        (cx, a_top),
        (cx, a_mid_cross),
        a_left_inner,
        a_left_tip,
        (cx - 140 * s, cy - 105 * s),
        (cx, a_top)
    ]
    m_draw.polygon(left_a_wing, fill=(2, 132, 199, 255)) # #0284C7
    
    # Right A-Wing (Electric Cyan)
    right_a_wing = [
        (cx, a_top),
        (cx, a_mid_cross),
        a_right_inner,
        a_right_tip,
        (cx + 140 * s, cy - 105 * s),
        (cx, a_top)
    ]
    m_draw.polygon(right_a_wing, fill=(6, 182, 212, 255)) # #06B6D4
    
    # Specular High-Light on Right Wing
    top_spec = [
        (cx, a_top),
        (cx + 140 * s, cy - 105 * s),
        (cx + 75 * s, cy - 50 * s),
        (cx, cy - 135 * s)
    ]
    m_draw.polygon(top_spec, fill=(34, 211, 238, 255)) # Glowing Cyan #22D3EE
    
    # Deep Shadow Bevel on Left Leg
    left_shd = [
        a_left_inner,
        a_left_tip,
        (cx - 115 * s, cy + 15 * s),
        (cx - 45 * s, cy + 15 * s)
    ]
    m_draw.polygon(left_shd, fill=(3, 105, 161, 255)) # Deep Ocean #0369A1
    
    # Inner Obsidian Vault Cutout
    vault_cut = [
        (cx, cy - 80 * s),
        (cx + 72 * s, cy + 120 * s),
        (cx, cy + 165 * s),
        (cx - 72 * s, cy + 120 * s)
    ]
    m_draw.polygon(vault_cut, fill=(10, 16, 26, 255))
    
    # Central Platinum Diamond (Cryptographic Trust Anchor)
    dia_pts = [
        (cx, cy - 35 * s),
        (cx + 42 * s, cy + 32 * s),
        (cx, cy + 98 * s),
        (cx - 42 * s, cy + 32 * s)
    ]
    
    # Subtle diamond cyan aura
    dia_aura = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(dia_aura).polygon(dia_pts, fill=(0, 229, 255, 150))
    dia_aura = dia_aura.filter(ImageFilter.GaussianBlur(radius=10 * s))
    mark = Image.alpha_composite(mark, dia_aura)
    m_draw = ImageDraw.Draw(mark)
    
    # Diamond left: Pure Platinum #E2E8F0
    m_draw.polygon([dia_pts[0], dia_pts[3], dia_pts[2]], fill=(226, 232, 240, 255))
    # Diamond right: Pure White #FFFFFF
    m_draw.polygon([dia_pts[0], dia_pts[1], dia_pts[2]], fill=(255, 255, 255, 255))
    
    # Central spine light highlights
    m_draw.line([(cx, a_top), (cx, cy - 80 * s)], fill=(255, 255, 255, 255), width=int(2.5 * s))
    m_draw.line([(cx, cy - 35 * s), (cx, cy + 98 * s)], fill=(255, 255, 255, 255), width=int(2 * s))
    
    # Subtle perimeter rim on "A"
    m_draw.polygon(left_a_wing, outline=(255, 255, 255, 120), width=int(1.5 * s))
    m_draw.polygon(right_a_wing, outline=(255, 255, 255, 180), width=int(1.5 * s))
    
    # 4. Lower Keel Chevron (Dual DNS Racing Indicator)
    keel_pts = [
        (cx, cy + 205 * s),
        (cx + 105 * s, cy + 142 * s),
        (cx + 135 * s, cy + 180 * s),
        (cx, cy + 278 * s),
        (cx - 135 * s, cy + 180 * s),
        (cx - 105 * s, cy + 142 * s)
    ]
    # Keel Left: Deep Sapphire
    m_draw.polygon([keel_pts[0], keel_pts[3], keel_pts[4], keel_pts[5]], fill=(2, 132, 199, 255))
    # Keel Right: Electric Cyan
    m_draw.polygon([keel_pts[0], keel_pts[1], keel_pts[2], keel_pts[3]], fill=(6, 182, 212, 255))
    m_draw.line([(cx, cy + 205 * s), (cx, cy + 278 * s)], fill=(255, 255, 255, 220), width=int(2 * s))
    
    img = Image.alpha_composite(img, mark)
    final = img.resize((size, size), Image.Resampling.LANCZOS)
    return final

def export_all():
    base_res = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    print("Rendering 1024x1024 Master Icon...")
    master = render_master_aegis(1024, is_foreground_only=False)
    master.save(os.path.join(base_res, "..", "..", "master_aegis_logo_512.png").replace("..\\..\\", ""), "PNG")
    
    # Render Foreground for Adaptive Icon
    print("Rendering 1024x1024 Adaptive Foreground...")
    foreground = render_master_aegis(1024, is_foreground_only=True)
    
    # Export to mipmap directories
    for folder, dim in densities.items():
        folder_path = os.path.join(base_res, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Standard Launcher Icon
        icon_resized = master.resize((dim, dim), Image.Resampling.LANCZOS)
        icon_path = os.path.join(folder_path, "ic_launcher.png")
        icon_resized.save(icon_path, "PNG")
        
        # Round Launcher Icon
        round_path = os.path.join(folder_path, "ic_launcher_round.png")
        icon_resized.save(round_path, "PNG")
        
        # Adaptive Foreground (Standard adaptive icon is 108dp x 108dp, so dimension is dim * 108 / 48)
        fg_dim = int(dim * 108 / 48)
        fg_resized = foreground.resize((fg_dim, fg_dim), Image.Resampling.LANCZOS)
        fg_path = os.path.join(folder_path, "ic_launcher_foreground.png")
        fg_resized.save(fg_path, "PNG")
        
        print(f"Exported {folder}: {dim}x{dim} and foreground {fg_dim}x{fg_dim}")
        
    # Also save to user Downloads and Brain artifact directory
    downloads_path = r"C:\Users\athul_nuy2ni9\Downloads\aegisguard_brand_logo.png"
    master.save(downloads_path, "PNG")
    
    brain_path = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3\aegisguard_brand_logo.png"
    master.save(brain_path, "PNG")
    
    print("All mipmap densities exported successfully!")

if __name__ == "__main__":
    export_all()
