import os, re

def check_path_data(file_path, path_str):
    tokens = re.findall(r'([A-Za-z])|(-?[0-9.]+)', path_str)
    cmd = None
    args = []
    expected = {
        'M': 2, 'm': 2,
        'L': 2, 'l': 2,
        'H': 1, 'h': 1,
        'V': 1, 'v': 1,
        'C': 6, 'c': 6,
        'S': 4, 's': 4,
        'Q': 4, 'q': 4,
        'T': 2, 't': 2,
        'A': 7, 'a': 7,
        'Z': 0, 'z': 0
    }
    errors = []
    
    def validate_segment(c, a):
        exp = expected.get(c, -1)
        if exp == 0:
            if len(a) > 0:
                errors.append(f"Command {c} expects 0 args, got {len(a)}")
        elif exp > 0:
            if len(a) == 0 or len(a) % exp != 0:
                errors.append(f"Command {c} expects multiple of {exp} args, got {len(a)} ({a})")

    for letter, num in tokens:
        if letter:
            if cmd:
                validate_segment(cmd, args)
            cmd = letter
            args = []
        else:
            args.append(float(num))
    if cmd:
        validate_segment(cmd, args)
    return errors

res_dir = r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res"
total_paths = 0
found_errors = False
for root, dirs, files in os.walk(res_dir):
    for f in files:
        if f.endswith(".xml"):
            p = os.path.join(root, f)
            with open(p, "r", encoding="utf-8") as xf:
                content = xf.read()
            paths = re.findall(r'android:pathData="([^"]+)"', content)
            for path_str in paths:
                total_paths += 1
                errs = check_path_data(p, path_str)
                if errs:
                    print(f"ERROR in {f}: {errs}")
                    print(f"Offending path: {path_str}")
                    found_errors = True

print(f"Total paths checked: {total_paths}")
if not found_errors:
    print("ALL PATHS ARE 100% VALID!")
