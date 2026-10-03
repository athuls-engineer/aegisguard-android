import os
from PIL import Image, ImageDraw, ImageFilter

from generate_shield_check_logo import create_shield_logo

def export_emerald_brand_icons():
    base_res = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    print("Generating 1024x1024 Master Classic Shield Checkmark...")
    master = create_shield_logo(1024, style="emerald", is_foreground_only=False)
    
    print("Generating 1024x1024 Adaptive Foreground...")
    foreground = create_shield_logo(1024, style="emerald", is_foreground_only=True)
    
    for folder, dim in densities.items():
        folder_path = os.path.join(base_res, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Standard Launcher Icon
        icon_resized = master.resize((dim, dim), Image.Resampling.LANCZOS)
        icon_resized.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
        
        # Round Launcher Icon
        icon_resized.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
        
        # Adaptive Foreground (108dp base canvas)
        fg_dim = int(dim * 108 / 48)
        fg_resized = foreground.resize((fg_dim, fg_dim), Image.Resampling.LANCZOS)
        fg_resized.save(os.path.join(folder_path, "ic_launcher_foreground.png"), "PNG")
        
        print(f"Exported {folder}: {dim}x{dim}")
        
    # Copy master to Downloads and Brain
    dl_path = r"C:\Users\athul_nuy2ni9\Downloads\AegisGuard-Logo-Classic.png"
    master.save(dl_path, "PNG")
    master.save(r"C:\Users\athul_nuy2ni9\Downloads\aegisguard_brand_logo.png", "PNG")
    
    brain_path = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3\aegisguard_brand_logo.png"
    master.save(brain_path, "PNG")
    master.save(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3\AegisGuard-Logo-Classic.png", "PNG")
    
    print("Master icons saved to Downloads and Brain successfully!")

if __name__ == "__main__":
    export_emerald_brand_icons()
