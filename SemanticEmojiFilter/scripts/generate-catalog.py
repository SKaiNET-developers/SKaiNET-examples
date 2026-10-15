#!/usr/bin/env python3
"""Builds catalog.json from Unicode CLDR emoji annotations and the curated selection.

    python3 -I scripts/generate-catalog.py --cldr <dir> --selection scripts/catalog-selection.json \
        --out composeApp/src/commonMain/composeResources/files/catalog/catalog.json

<dir> holds the CLDR files common/annotations/{en,de}.xml and common/annotationsDerived/{en,de}.xml
of one release (default names: annotations_en.xml, annotations_de.xml, annotationsDerived_en.xml,
annotationsDerived_de.xml). Names come from the `tts` annotation, keywords from the plain one.
The script fails on a missing annotation or a duplicate id, so the catalog is always complete.
"""
import argparse
import json
import sys
import xml.etree.ElementTree as ET

VS16 = "️"


def load_annotations(path):
    names, keywords = {}, {}
    root = ET.parse(path).getroot()
    for node in root.iter("annotation"):
        cp = node.get("cp", "").replace(VS16, "")
        text = (node.text or "").strip()
        if node.get("type") == "tts":
            names[cp] = text
        else:
            keywords[cp] = [k.strip() for k in text.split("|") if k.strip()]
    return names, keywords


def merge(a, b):
    out = dict(a)
    out.update(b)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cldr", required=True)
    ap.add_argument("--cldr-release", default="47")
    ap.add_argument("--selection", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--embedding-text-version", type=int, default=1)
    args = ap.parse_args()

    en_names, en_kw = load_annotations(f"{args.cldr}/annotations_en.xml")
    de_names, de_kw = load_annotations(f"{args.cldr}/annotations_de.xml")
    den, dek = load_annotations(f"{args.cldr}/annotationsDerived_en.xml")
    ddn, ddk = load_annotations(f"{args.cldr}/annotationsDerived_de.xml")
    en_names, en_kw = merge(en_names, den), merge(en_kw, dek)
    de_names, de_kw = merge(de_names, ddn), merge(de_kw, ddk)

    with open(args.selection, encoding="utf-8") as f:
        selection = json.load(f)

    items, seen, errors = [], set(), []
    for entry in selection:
        cps = [int(h, 16) for h in entry["cp"].split("-")]
        emoji = "".join(chr(c) for c in cps)
        key = emoji.replace(VS16, "")
        item_id = "-".join(f"{c:04X}" for c in cps if c != 0xFE0F)
        if item_id in seen:
            errors.append(f"duplicate id {item_id}")
            continue
        seen.add(item_id)
        if key not in en_names or key not in de_names:
            errors.append(f"{item_id} ({emoji}): no CLDR name in {'en' if key not in en_names else 'de'}")
            continue
        items.append({
            "id": item_id,
            "emoji": emoji,
            "nameEn": en_names[key],
            "nameDe": de_names[key],
            "keywordsEn": en_kw.get(key, []),
            "keywordsDe": de_kw.get(key, []),
            "tags": entry.get("tags", []),
            "group": entry.get("group", ""),
        })

    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)

    catalog = {
        "version": 1,
        "cldr": args.cldr_release,
        "embeddingTextVersion": args.embedding_text_version,
        "embeddingsSha256": "",
        "items": items,
    }
    # Keep an existing digest if the items did not change (the CLI writes it after embedding).
    try:
        with open(args.out, encoding="utf-8") as f:
            previous = json.load(f)
        if previous.get("items") == items and previous.get("embeddingTextVersion") == args.embedding_text_version:
            catalog["embeddingsSha256"] = previous.get("embeddingsSha256", "")
    except (OSError, ValueError):
        pass
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"catalog: {len(items)} items -> {args.out}")


if __name__ == "__main__":
    main()
