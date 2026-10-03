import re

with open(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res\layout\activity_main.xml", "r", encoding="utf-8") as f:
    main_xml = f.read()
xml_ids = set(re.findall(r'android:id="@\+id/([a-zA-Z0-9_]+)"', main_xml))

with open(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\java\org\aegisguard\android\MainActivity.java", "r", encoding="utf-8") as f:
    main_java = f.read()
java_ids = set(re.findall(r'findViewById\(R\.id\.([a-zA-Z0-9_]+)\)', main_java))

print("MainActivity IDs checked:")
missing = False
for jid in sorted(java_ids):
    if jid not in xml_ids:
        print("  MISSING IN XML:", jid)
        missing = True
    else:
        print("  OK:", jid)

if not missing:
    print("ALL MainActivity IDs EXIST!")

# Settings Activity
with open(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\res\layout\activity_settings.xml", "r", encoding="utf-8") as f:
    settings_xml = f.read()
settings_xml_ids = set(re.findall(r'android:id="@\+id/([a-zA-Z0-9_]+)"', settings_xml))

with open(r"C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android\src\main\java\org\aegisguard\android\SettingsActivity.java", "r", encoding="utf-8") as f:
    settings_java = f.read()
settings_java_ids = set(re.findall(r'findViewById\(R\.id\.([a-zA-Z0-9_]+)\)', settings_java))

print("\nSettingsActivity IDs checked:")
settings_missing = False
for jid in sorted(settings_java_ids):
    if jid not in settings_xml_ids:
        print("  MISSING IN XML:", jid)
        settings_missing = True
    else:
        print("  OK:", jid)

if not settings_missing:
    print("ALL SettingsActivity IDs EXIST!")
