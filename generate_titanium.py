import math
from PIL import Image, ImageDraw, ImageFilter

def create_obsidian_titanium_aegis(size=1024):
    scale = 2
    W, H = size * scale, size * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s = scale
    cx, cy = W / 2.0, H / 2.0
    
    # 1. Background Squircle Canvas
    pad = int(44 * s)
    corner_r = int(224 * s)
    
    bg_mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(bg_mask).rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    # Deep luxury obsidian gradient: #080C14 to #0E1526
    bg = Image.new("RGBA", (W, H), (8, 12, 20, 255))
    
    # Subtle deep radial ambient glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    for r in range(int(460 * s), 0, -8):
        a = int(32 * (1.0 - (r / (460 * s)) ** 1.8))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(6, 182, 212, a))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=24 * s))
    bg = Image.alpha_composite(bg, glow)
    
    # Precise perimeter satin rim
    b_draw = ImageDraw.Draw(bg)
    b_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, outline=(30, 41, 59, 240), width=int(2.5 * s))
    img.paste(bg, (0, 0), bg_mask)
    
    # 2. Master Shield Contour (Golden Ratio Aerodynamic Geometry)
    # Apex top: (cx, cy - 315*s)
    # Shoulder left: (cx - 245*s, cy - 185*s)
    # Shoulder right: (cx + 245*s, cy - 185*s)
    # Mid-waist left: (cx - 235*s, cy + 90*s)
    # Mid-waist right: (cx + 235*s, cy + 90*s)
    # Keel point bottom: (cx, cy + 345*s)
    
    shield_pts = [
        (cx, cy - 315 * s),
        (cx + 245 * s, cy - 185 * s),
        (cx + 235 * s, cy + 90 * s),
        (cx, cy + 345 * s),
        (cx - 235 * s, cy + 90 * s),
        (cx - 245 * s, cy - 185 * s)
    ]
    
    # Deep ambient drop shadow under shield
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(shield_pts, fill=(0, 0, 0, 210))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=32 * s))
    img = Image.alpha_composite(img, shadow)
    
    mark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Titanium Shield Body - Left Facet (Deep Matte Charcoal Slate)
    left_body = [
        (cx, cy - 315 * s),
        (cx - 245 * s, cy - 185 * s),
        (cx - 235 * s, cy + 90 * s),
        (cx, cy + 345 * s),
        (cx, cy - 315 * s)
    ]
    m_draw.polygon(left_body, fill=(17, 24, 39, 255)) # Slate 900
    
    # Titanium Shield Body - Right Facet (Refined Gunmetal)
    right_body = [
        (cx, cy - 315 * s),
        (cx + 245 * s, cy - 185 * s),
        (cx + 235 * s, cy + 90 * s),
        (cx, cy + 345 * s),
        (cx, cy - 315 * s)
    ]
    m_draw.polygon(right_body, fill=(24, 33, 50, 255)) # Elevated metallic slate
    
    # Outer Chamfered Bevel Rim (Aerospace Titanium Finish)
    m_draw.polygon(shield_pts, outline=(51, 65, 85, 240), width=int(3 * s))
    
    # Specular light edge on top ridges
    m_draw.line([(cx, cy - 315 * s), (cx - 245 * s, cy - 185 * s)], fill=(148, 163, 184, 180), width=int(2 * s))
    m_draw.line([(cx, cy - 315 * s), (cx + 245 * s, cy - 185 * s)], fill=(248, 250, 252, 240), width=int(2.5 * s))
    
    # Center vertical titanium crease line
    m_draw.line([(cx, cy - 315 * s), (cx, cy + 345 * s)], fill=(71, 85, 105, 160), width=int(2 * s))
    
    # 3. The Hero "Apex Vanguard A" - Multi-Faceted Luminescent Core
    # This is the blazing centerpiece: A sharp, monolithic chevron monogram
    
    # Left Wing of the "A" (Deep Sapphire to Ocean Cyan)
    left_a_wing = [
        (cx, cy - 210 * s),
        (cx, cy - 90 * s),
        (cx - 85 * s, cy + 130 * s),
        (cx - 155 * s, cy + 130 * s),
        (cx - 135 * s, cy - 100 * s),
        (cx, cy - 210 * s)
    ]
    m_draw.polygon(left_a_wing, fill=(2, 132, 199, 255)) # Sapphire #0284C7
    
    # Right Wing of the "A" (Brilliant Electric Cyan Luminescence)
    right_a_wing = [
        (cx, cy - 210 * s),
        (cx, cy - 90 * s),
        (cx + 85 * s, cy + 130 * s),
        (cx + 155 * s, cy + 130 * s),
        (cx + 135 * s, cy - 100 * s),
        (cx, cy - 210 * s)
    ]
    m_draw.polygon(right_a_wing, fill=(6, 182, 212, 255)) # Cyan #06B6D4
    
    # Top Specular Crest on Right A-Wing
    top_spec = [
        (cx, cy - 210 * s),
        (cx + 135 * s, cy - 100 * s),
        (cx + 70 * s, cy - 50 * s),
        (cx, cy - 130 * s)
    ]
    m_draw.polygon(top_spec, fill=(34, 211, 238, 255)) # Glowing Cyan #22D3EE
    
    # Left Wing Shadow Accent
    left_shd = [
        (cx - 85 * s, cy + 130 * s),
        (cx - 155 * s, cy + 130 * s),
        (cx - 110 * s, cy + 15 * s),
        (cx - 40 * s, cy + 15 * s)
    ]
    m_draw.polygon(left_shd, fill=(3, 105, 161, 255)) # Deep Ocean #0369A1
    
    # Negative Space Inside A (Titanium Core Vault)
    vault_cut = [
        (cx, cy - 80 * s),
        (cx + 70 * s, cy + 115 * s),
        (cx, cy + 160 * s),
        (cx - 70 * s, cy + 115 * s)
    ]
    m_draw.polygon(vault_cut, fill=(12, 18, 28, 255))
    
    # Central Floating Platinum Diamond (The Cryptographic Trust Key)
    dia_core = [
        (cx, cy - 35 * s),
        (cx + 42 * s, cy + 30 * s),
        (cx, cy + 95 * s),
        (cx - 42 * s, cy + 30 * s)
    ]
    # Diamond shadow
    dia_shd = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(dia_shd).polygon(dia_core, fill=(0, 229, 255, 140))
    dia_shd = dia_shd.filter(ImageFilter.GaussianBlur(radius=10 * s))
    mark = Image.alpha_composite(mark, dia_shd)
    
    m_draw = ImageDraw.Draw(mark)
    # Diamond left half: Pure Platinum
    m_draw.polygon([dia_core[0], dia_core[3], dia_core[2]], fill=(226, 232, 240, 255))
    # Diamond right half: Brilliant Pure White
    m_draw.polygon([dia_core[0], dia_core[1], dia_core[2]], fill=(255, 255, 255, 255))
    
    # Razor-sharp central spine
    m_draw.line([(cx, cy - 210 * s), (cx, cy - 80 * s)], fill=(255, 255, 255, 255), width=int(2.5 * s))
    m_draw.line([(dia_core[0][0], dia_core[0][1]), (dia_core[2][0], dia_core[2][1])], fill=(255, 255, 255, 255), width=int(2 * s))
    
    # Hairline crisp strokes on the "A"
    m_draw.polygon(left_a_wing, outline=(255, 255, 255, 120), width=int(1.5 * s))
    m_draw.polygon(right_a_wing, outline=(255, 255, 255, 180), width=int(1.5 * s))
    
    # 4. Lower Keel Chevron Accent (Split-DNS Engine Indicator)
    keel_pts = [
        (cx, cy + 200 * s),
        (cx + 100 * s, cy + 140 * s),
        (cx + 130 * s, cy + 175 * s),
        (cx, cy + 270 * s),
        (cx - 130 * s, cy + 175 * s),
        (cx - 100 * s, cy + 140 * s)
    ]
    # Keel left (Deep Cyan)
    m_draw.polygon([keel_pts[0], keel_pts[3], keel_pts[4], keel_pts[5]], fill=(2, 132, 199, 255))
    # Keel right (Electric Cyan)
    m_draw.polygon([keel_pts[0], keel_pts[1], keel_pts[2], keel_pts[3]], fill=(6, 182, 212, 255))
    m_draw.line([(cx, cy + 200 * s), (cx, cy + 270 * s)], fill=(255, 255, 255, 220), width=int(2 * s))
    
    img = Image.alpha_composite(img, mark)
    return img.resize((size, size), Image.Resampling.LANCZOS)

if __name__ == "__main__":
    titan = create_obsidian_titanium_aegis(1024)
    titan.save("logo_obsidian_titanium.png", "PNG")
    print("Generated logo_obsidian_titanium.png")
