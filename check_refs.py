import xml.etree.ElementTree as ET
import os, re

res_dir = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res"
layout_path = os.path.join(res_dir, "layout", "activity_main.xml")

with open(layout_path, "r", encoding="utf-8") as f:
    content = f.read()

refs = re.findall(r'@(drawable|color|string|mipmap)/([a-zA-Z0-9_]+)', content)
print(f"Total refs found: {len(refs)}")

missing = []
for res_type, name in set(refs):
    found = False
    if res_type == "drawable":
        for ext in [".xml", ".png", ".jpg", ".webp"]:
            p = os.path.join(res_dir, "drawable", name + ext)
            if os.path.exists(p):
                found = True
                break
    elif res_type == "mipmap":
        for folder in ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"]:
            for ext in [".xml", ".png", ".jpg", ".webp"]:
                p = os.path.join(res_dir, folder, name + ext)
                if os.path.exists(p):
                    found = True
                    break
    elif res_type in ["color", "string"]:
        val_file = os.path.join(res_dir, "values", res_type + "s.xml")
        if os.path.exists(val_file):
            with open(val_file, "r", encoding="utf-8") as vf:
                vcontent = vf.read()
                if f'name="{name}"' in vcontent:
                    found = True
    if not found:
        missing.append(f"@{res_type}/{name}")

print("Missing resources:", missing)
