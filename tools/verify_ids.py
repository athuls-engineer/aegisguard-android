import os
import re

with open(os.path.join(os.path.dirname(__file__), "..", "src", "main", "res", "layout", "activity_main.xml"), "r", encoding="utf-8") as f:
    main_xml = f.read()
xml_ids = set(re.findall(r'android:id="@\+id/([a-zA-Z0-9_]+)"', main_xml))

with open(os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "org", "aegisguard", "android", "MainActivity.java"), "r", encoding="utf-8") as f:
    main_java = f.read()
java_ids = set(re.findall(r'findViewById\(R\.id\.([a-zA-Z0-9_]+)\)', main_java))
java_ids.update(re.findall(r'R\.id\.([a-zA-Z0-9_]+)', main_java))

missing_in_xml = java_ids - xml_ids
# Filter out standard system IDs or non-activity_main IDs if any
print(f"Total XML IDs: {len(xml_ids)}, Java referenced IDs: {len(java_ids)}")
print(f"Missing in XML: {missing_in_xml}")
