import math
from PIL import Image, ImageDraw, ImageFilter

def create_aegis_logo(size=1024):
    # Render at 2x scale (2048x2048) for super-sampled anti-aliasing
    scale = 2
    W = size * scale
    H = size * scale
    
    # Base image with deep midnight canvas
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # 1. Rounded squircle background
    pad = int(48 * scale)
    corner_r = int(220 * scale)
    
    # Create radial background in squircle mask
    bg_mask = Image.new("L", (W, H), 0)
    mask_draw = ImageDraw.Draw(bg_mask)
    mask_draw.rounded_rectangle([pad, pad, W - pad, H - pad], radius=corner_r, fill=255)
    
    # Radial dark slate-navy gradient
    bg_layer = Image.new("RGBA", (W, H), (8, 12, 20, 255))
    center_x, center_y = W // 2, int(H * 0.48)
    
    # Glow in center
    glow_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_radius = int(420 * scale)
    for r in range(glow_radius, 0, -8):
        alpha = int(45 * (1.0 - (r / glow_radius) ** 1.5))
        glow_draw.ellipse(
            [center_x - r, center_y - r, center_x + r, center_y + r],
            fill=(14, 165, 233, alpha)
        )
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(radius=16 * scale))
    bg_layer = Image.alpha_composite(bg_layer, glow_layer)
    
    # Subtle border ring around squircle
    border_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    border_draw = ImageDraw.Draw(border_layer)
    border_draw.rounded_rectangle(
        [pad, pad, W - pad, H - pad],
        radius=corner_r,
        outline=(51, 65, 85, 200),
        width=int(3 * scale)
    )
    bg_layer = Image.alpha_composite(bg_layer, border_layer)
    
    # Composite background into img
    img.paste(bg_layer, (0, 0), bg_mask)
    
    # 2. Geometric Shield Mark (The Apex Aegis)
    # Master geometric points
    cx = W / 2.0
    cy = H / 2.0
    
    # Shield proportions
    # Apex top: (cx, cy - 280*s)
    # Top shoulders: (cx - 260*s, cy - 170*s) and (cx + 260*s, cy - 170*s)
    # Mid hip: (cx - 240*s, cy + 60*s) and (cx + 240*s, cy + 60*s)
    # Bottom tip: (cx, cy + 320*s)
    
    s = scale
    
    # Soft drop shadow behind mark
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    outer_poly = [
        (cx, cy - 260 * s),
        (cx + 230 * s, cy - 160 * s),
        (cx + 210 * s, cy + 50 * s),
        (cx, cy + 290 * s),
        (cx - 210 * s, cy + 50 * s),
        (cx - 230 * s, cy - 160 * s)
    ]
    s_draw.polygon(outer_poly, fill=(0, 0, 0, 140))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=25 * s))
    img = Image.alpha_composite(img, shadow)
    
    # Now draw the pristine faceted mark components
    mark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(mark)
    
    # Outer Facets (Dark Titanium Sapphire Wings)
    # Left Outer Wing
    left_outer = [
        (cx, cy - 260 * s),
        (cx - 230 * s, cy - 160 * s),
        (cx - 210 * s, cy + 50 * s),
        (cx, cy + 290 * s),
        (cx, cy + 220 * s),
        (cx - 160 * s, cy + 35 * s),
        (cx - 175 * s, cy - 125 * s),
        (cx, cy - 200 * s)
    ]
    # Gradient on left outer
    m_draw.polygon(left_outer, fill=(15, 23, 42, 255))
    
    # Right Outer Wing
    right_outer = [
        (cx, cy - 260 * s),
        (cx + 230 * s, cy - 160 * s),
        (cx + 210 * s, cy + 50 * s),
        (cx, cy + 290 * s),
        (cx, cy + 220 * s),
        (cx + 160 * s, cy + 35 * s),
        (cx + 175 * s, cy - 125 * s),
        (cx, cy - 200 * s)
    ]
    m_draw.polygon(right_outer, fill=(20, 31, 52, 255))
    
    # Outer border stroke for crisp clarity
    m_draw.polygon(outer_poly, outline=(34, 211, 238, 220), width=int(3.5 * s))
    
    # 3. Inner "A" Vanguard Spear / Chevron (The Aegis Core)
    # Left A-facet: Ice Cyan to Deep Cyan
    left_a = [
        (cx, cy - 170 * s),
        (cx, cy + 120 * s),
        (cx - 65 * s, cy + 80 * s),
        (cx - 130 * s, cy - 70 * s),
        (cx, cy - 170 * s)
    ]
    # Right A-facet: Crisp Diamond Cyan to Azure
    right_a = [
        (cx, cy - 170 * s),
        (cx, cy + 120 * s),
        (cx + 65 * s, cy + 80 * s),
        (cx + 130 * s, cy - 70 * s),
        (cx, cy - 170 * s)
    ]
    
    m_draw.polygon(left_a, fill=(6, 182, 212, 255))
    m_draw.polygon(right_a, fill=(34, 211, 238, 255))
    
    # Inner Cutout (Negative space forming the letter "A" and diamond vault)
    inner_cut = [
        (cx, cy - 100 * s),
        (cx + 50 * s, cy + 30 * s),
        (cx, cy + 65 * s),
        (cx - 50 * s, cy + 30 * s)
    ]
    m_draw.polygon(inner_cut, fill=(9, 13, 22, 255))
    
    # Inner floating Diamond Prism (The Security Anchor)
    diamond = [
        (cx, cy - 40 * s),
        (cx + 28 * s, cy),
        (cx, cy + 40 * s),
        (cx - 28 * s, cy)
    ]
    m_draw.polygon(diamond, fill=(248, 250, 252, 255))
    
    # Central spine precision line
    m_draw.line([(cx, cy - 170 * s), (cx, cy - 100 * s)], fill=(255, 255, 255, 230), width=int(2.5 * s))
    m_draw.line([(cx, cy + 65 * s), (cx, cy + 120 * s)], fill=(255, 255, 255, 180), width=int(2 * s))
    m_draw.line([(cx, cy + 220 * s), (cx, cy + 290 * s)], fill=(34, 211, 238, 220), width=int(2.5 * s))
    
    # Top ridge highlights
    m_draw.line([(cx, cy - 260 * s), (cx - 230 * s, cy - 160 * s)], fill=(255, 255, 255, 140), width=int(2 * s))
    m_draw.line([(cx, cy - 260 * s), (cx + 230 * s, cy - 160 * s)], fill=(255, 255, 255, 180), width=int(2 * s))
    
    img = Image.alpha_composite(img, mark)
    
    # Final downsample with Lanczos for ultra-crisp edges
    final = img.resize((size, size), Image.Resampling.LANCZOS)
    return final

if __name__ == "__main__":
    icon = create_aegis_logo(1024)
    icon.save("test_logo.png", "PNG")
    print("Generated test_logo.png successfully")
