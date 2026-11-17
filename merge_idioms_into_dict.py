import sys

dict_path = "app/src/main/assets/internal_ej_dict.tsv"
idioms_path = "idioms_5000.tsv"

print(f"Reading existing dictionary from {dict_path}...")
entries = {}

with open(dict_path, "r", encoding="utf-8") as f:
    for line in f:
        line = line.rstrip("\r\n")
        if not line:
            continue
        parts = line.split("\t")
        if len(parts) >= 3:
            key = parts[0].strip().lower()
            entries[key] = line

print(f"Loaded {len(entries)} existing entries.")

print(f"Reading idioms from {idioms_path}...")
added = 0
updated = 0
with open(idioms_path, "r", encoding="utf-8") as f:
    for line in f:
        line = line.rstrip("\r\n")
        if not line:
            continue
        parts = line.split("\t")
        if len(parts) >= 3:
            key = parts[0].strip().lower()
            if key in entries:
                # Update with richer idiom definition and note
                entries[key] = line
                updated += 1
            else:
                entries[key] = line
                added += 1

print(f"Added {added} new idioms, updated {updated} existing idioms. Total entries: {len(entries)}")

print(f"Writing sorted dictionary back to {dict_path}...")
with open(dict_path, "w", encoding="utf-8") as f:
    for key in sorted(entries.keys()):
        f.write(entries[key] + "\n")

print("Merge complete!")
