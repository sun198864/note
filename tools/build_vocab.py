#!/usr/bin/env python3
"""Build app/src/main/assets/vocab.json from the CEEC 111 vocabulary list.

The alphabetical section of the extracted PDF supplies the headword, part of
speech, and level. Phonetics and Chinese glosses come from ECDICT. Example
sentences come from Tatoeba. Neither source is copied in full.
"""

from __future__ import annotations

import argparse
import bz2
import hashlib
import json
import re
import sqlite3
from pathlib import Path

import zhconv

POS_TAG = (
    r"(?:\()?(?:art|adj|adv|prep|conj|pron|aux|int|abbr|num|det|interj|pl|n|v)\.(?:\))?"
)
ENTRY_RE = re.compile(
    rf"^(?P<word>.+?)\s+(?P<pos>{POS_TAG}(?:\s*/\s*{POS_TAG})*)\s*(?P<level>[1-6])$"
)
TOKEN_RE = re.compile(r"[A-Za-z]+(?:[.'’-][A-Za-z]+)*")
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
MEANING_OVERRIDES = {
    "download": "下載",
    "upload": "上傳",
    "online": "線上",
    "Internet/internet": "網際網路",
    "hacker": "駭客",
    "steal": "偷竊",
}
POS_TAGS_RE = re.compile(
    r"art|adj|adv|prep|conj|pron|aux|int|abbr|num|det|interj|pl|n|v"
)


def join_buffer(buffer: str, line: str) -> str:
    if not buffer:
        return line
    if buffer.endswith("/") or buffer.endswith("-"):
        return buffer + line
    broken = re.match(r"^([a-z])\s+(.*)$", line)
    if broken and buffer[-1].isalpha():
        return f"{buffer}{broken.group(1)} {broken.group(2)}"
    return f"{buffer} {line}"


def is_noise(line: str) -> bool:
    if line.startswith("依字母排序") or line.startswith("附錄") or line.startswith("#"):
        return True
    if line.isdigit() and len(line) >= 2:
        return True
    if re.fullmatch(r"[A-Z]", line):
        return True
    return False


def parse_entries(text: str) -> tuple[list[dict], list[str]]:
    lines = text.splitlines()
    start = next(
        (index for index, line in enumerate(lines) if line.strip() == "a/an art.1"),
        None,
    )
    if start is None:
        raise SystemExit("Could not find the alphabetical vocabulary section")

    entries: list[dict] = []
    errors: list[str] = []
    buffer = ""
    for raw in lines[start:]:
        line = raw.strip()
        if not line:
            continue
        if line.startswith("附錄"):
            break
        line = line.replace("’", "'").replace("‘", "'")
        line = re.sub(
            r"(art|adj|adv|prep|conj|pron|aux|int|abbr|num|det|interj|pl|n|v)([1-6])$",
            r"\1. \2",
            line,
        )
        if re.fullmatch(r"[1-6]", line):
            match = ENTRY_RE.match(join_buffer(buffer, line)) if buffer else None
            if match:
                entries.append(entry_from_match(match))
                buffer = ""
            continue
        if is_noise(line):
            if buffer:
                errors.append(buffer)
                buffer = ""
            continue
        candidate = join_buffer(buffer, line)
        match = ENTRY_RE.match(candidate)
        if match:
            entries.append(entry_from_match(match))
            buffer = ""
            continue
        if buffer and ENTRY_RE.match(line):
            errors.append(buffer)
            entries.append(entry_from_match(ENTRY_RE.match(line)))
            buffer = ""
            continue
        buffer = candidate
    if buffer:
        errors.append(buffer)
    return entries, errors


def entry_from_match(match: re.Match[str]) -> dict:
    word = re.sub(r"\s+", " ", match.group("word")).strip()
    word = word.replace("’", "'").replace("‘", "'")
    pos = re.sub(r"\s+", "", match.group("pos"))
    level = int(match.group("level"))
    raw_id = f"{word}\t{pos}\t{level}"
    entry_id = hashlib.sha1(raw_id.encode("utf-8")).hexdigest()[:12]
    return {
        "id": entry_id,
        "word": word,
        "pos": pos,
        "level": level,
    }


def headword_forms(word: str) -> list[str]:
    forms: list[str] = []
    for part in (piece.strip() for piece in word.split("/")):
        if not part:
            continue
        listed = re.fullmatch(r"(\S+)\s*\((.+)\)", part)
        if listed and "," in listed.group(2):
            forms.append(listed.group(1))
            forms.extend(piece.strip() for piece in listed.group(2).split(",") if piece.strip())
            continue
        suffix = re.fullmatch(r"([^(]+)\(([^)]+)\)", part)
        if suffix:
            base = suffix.group(1).strip()
            forms.append(base)
            forms.append(base + suffix.group(2).strip())
            continue
        forms.append(part)
    unique: list[str] = []
    for form in forms:
        if form and form not in unique:
            unique.append(form)
    return unique


def lookup_keys(form: str) -> list[str]:
    key = form.lower().replace("’", "'").replace("‘", "'")
    keys = [key]
    if key.endswith("."):
        keys.append(key[:-1])
    return keys


def normalize_phonetic(raw: str | None) -> str:
    if not raw:
        return ""
    text = raw.strip().replace("ә", "ə").replace("'", "ˈ").replace(":", "ː")
    text = re.sub(r"\s+", " ", text)
    if not text:
        return ""
    return text if text.startswith("/") else f"/{text}/"


def official_tags(pos: str) -> list[str]:
    return POS_TAGS_RE.findall(pos)


def line_matches(line: str, tag: str) -> bool:
    prefix = line.split(" ", 1)[0].lower().strip(".")
    if tag == "n":
        return prefix in {"n", "pl"}
    if tag == "v":
        return prefix in {"v", "vt", "vi", "vtvi"} or prefix.startswith("v")
    if tag == "adj":
        return prefix in {"a", "adj", "s", "j"}
    if tag == "adv":
        return prefix in {"adv", "r", "ad"}
    if tag == "art":
        return prefix == "art"
    if tag in {"prep", "conj", "pron", "aux", "int", "abbr", "num", "det", "interj"}:
        return prefix == tag
    return False


def clean_definition(definition: str | None, pos: str) -> str:
    if not definition:
        return ""
    lines = [re.sub(r"\s+", " ", line).strip() for line in definition.splitlines() if line.strip()]
    chosen = ""
    for tag in official_tags(pos):
        for line in lines:
            if line_matches(line, tag):
                chosen = line
                break
        if chosen:
            break
    if not chosen:
        return ""
    chosen = re.sub(r"^(?:n|v|vt|vi|a|adj|adv|s|r|pl)\.\s*", "", chosen, flags=re.IGNORECASE)
    chosen = re.sub(r"\s+", " ", chosen).strip()
    lowered = chosen.lower()
    if any(marker in lowered for marker in ("see the", "see thee", "dat.", "a word placed", "by that;")):
        return ""
    semicolon = chosen.find(";")
    if 20 <= semicolon <= 140:
        chosen = chosen[:semicolon].rstrip()
    elif len(chosen) > 110:
        cut = chosen.rfind(" ", 0, 110)
        chosen = chosen[:cut].rstrip() if cut >= 40 else chosen[:110].rstrip()
    return chosen


def glosses_from_line(line: str) -> tuple[list[str], bool]:
    domain = re.search(r"\[[^\]]*\]", line) is not None
    stripped = re.sub(r"\[[^\]]*\]", "", line)
    stripped = re.sub(r"^(?:[a-z]+\.\s*)+", "", stripped, flags=re.IGNORECASE)
    glosses: list[str] = []
    for piece in re.split(r"[,，;；、/]", stripped):
        gloss = zhconv.convert(piece.strip(), "zh-tw")
        gloss = gloss.replace("...", "…").strip(" 。.…")
        if gloss and CJK_RE.search(gloss) and gloss not in glosses:
            glosses.append(gloss)
    return glosses, domain


def clean_meaning(translation: str | None, pos: str) -> tuple[str, bool, bool]:
    if not translation:
        return "", False, False
    lines = [line.strip() for line in translation.replace("\\n", "\n").splitlines() if line.strip()]
    tags = official_tags(pos)
    glosses: list[str] = []
    matched = False
    from_domain = False
    for tag in tags:
        chosen: tuple[list[str], bool] | None = None
        for line in lines:
            if not line_matches(line, tag):
                continue
            picked, domain = glosses_from_line(line)
            if not picked:
                continue
            if chosen is None or (chosen[1] and not domain):
                chosen = (picked, domain)
            if not domain:
                break
        if chosen is None:
            continue
        matched = True
        from_domain = chosen[1]
        first = chosen[0][0]
        if tag == "art" and first == "一":
            first = "一個"
        elif tag == "art" and first == "那":
            first = "這；那"
        if first not in glosses:
            glosses.append(first)
        if len(glosses) == 3:
            break
    if not glosses:
        for line in lines:
            picked, domain = glosses_from_line(line)
            if picked:
                glosses.append(picked[0])
                from_domain = domain
                break
    return "；".join(glosses[:3]), matched, from_domain


def load_dictionary(entries: list[dict], database: Path) -> None:
    connection = sqlite3.connect(database)
    try:
        for entry in entries:
            best: tuple[tuple[int, int, int], str, str, str] | None = None
            for form in headword_forms(entry["word"]):
                row = None
                for key in lookup_keys(form):
                    row = connection.execute(
                        "SELECT phonetic, definition, translation FROM stardict WHERE word = ? COLLATE NOCASE",
                        (key,),
                    ).fetchone()
                    if row:
                        break
                if not row:
                    continue
                meaning, matched, from_domain = clean_meaning(row[2], entry["pos"])
                phonetic = normalize_phonetic(row[0])
                definition = clean_definition(row[1], entry["pos"])
                rank = (
                    1 if matched and meaning else 0,
                    0 if from_domain else 1,
                    1 if meaning else 0,
                    1 if phonetic else 0,
                )
                if best is None or rank > best[0]:
                    best = (rank, meaning, phonetic, definition)
            if best is None:
                entry["meaning"] = ""
                entry["phonetic"] = ""
                entry["definition"] = ""
                continue
            _, meaning, phonetic, definition = best
            entry["meaning"] = MEANING_OVERRIDES.get(entry["word"], meaning)
            entry["phonetic"] = phonetic
            entry["definition"] = definition
    finally:
        connection.close()


def inflection_keys(form: str) -> set[str]:
    key = form.lower()
    keys = {key}
    if len(key) < 4 or not key.isalpha():
        return keys
    keys.update({key + "s", key + "es", key + "ed", key + "d", key + "ing"})
    if key.endswith("e"):
        keys.add(key[:-1] + "ing")
    if key.endswith("y") and key[-2] not in "aeiou":
        keys.add(key[:-1] + "ies")
        keys.add(key[:-1] + "ied")
    if (
        len(key) > 3
        and key[-1] not in "aeiou"
        and key[-2] in "aeiou"
        and key[-3] not in "aeiou"
    ):
        keys.add(key + key[-1] + "ed")
        keys.add(key + key[-1] + "ing")
    return keys


def sentence_is_usable(text: str) -> bool:
    words = re.findall(r"[A-Za-z]+(?:'[A-Za-z]+)?", text)
    if not 5 <= len(words) <= 16:
        return False
    if text[0].isalpha() and not text[0].isupper():
        return False
    lowered = text.lower()
    if "http" in lowered or "www." in lowered:
        return False
    if any(mark in text for mark in "<>{}[]@#|\\_"):
        return False
    return True


def load_examples(entries: list[dict], sentences: Path) -> None:
    index: dict[str, list[tuple[int, int]]] = {}
    for entry_index, entry in enumerate(entries):
        entry["example"] = ""
        entry["exampleRank"] = (9, 10_000)
        for form in headword_forms(entry["word"]):
            exact = form.lower()
            inflected = inflection_keys(form)
            for token in inflected:
                index.setdefault(token, []).append((entry_index, 0 if token == exact else 1))

    with bz2.open(sentences, "rt", encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 3:
                continue
            text = parts[2].strip()
            if not text or not sentence_is_usable(text):
                continue
            tokens = {token.lower() for token in TOKEN_RE.findall(text)}
            if not tokens:
                continue
            length = len(text)
            seen: set[int] = set()
            for token in tokens:
                for entry_index, rank in index.get(token, ()):
                    if entry_index in seen:
                        continue
                    seen.add(entry_index)
                    current = entries[entry_index]["exampleRank"]
                    candidate = (rank, length)
                    if candidate < current:
                        entries[entry_index]["example"] = text
                        entries[entry_index]["exampleRank"] = candidate


def write_asset(entries: list[dict], output: Path) -> None:
    payload = [
        {
            "id": entry["id"],
            "word": entry["word"],
            "pos": entry["pos"],
            "level": entry["level"],
            "meaning": entry.get("meaning", ""),
            "phonetic": entry.get("phonetic", ""),
            "definition": entry.get("definition", ""),
            "example": entry.get("example", ""),
        }
        | ({"exampleZh": entry["exampleZh"]} if entry.get("exampleZh") else {})
        for entry in entries
    ]
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pdf-text", type=Path, required=True)
    parser.add_argument("--ecdict", type=Path, required=True)
    parser.add_argument("--sentences", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    entries, errors = parse_entries(args.pdf_text.read_text(encoding="utf-8"))
    print(f"entries={len(entries)} unmatched={len(errors)}")
    for error in errors[:40]:
        print("UNMATCHED", error)
    seen: set[str] = set()
    duplicates = 0
    for entry in entries:
        if entry["id"] in seen:
            duplicates += 1
        seen.add(entry["id"])
    print(f"duplicate_ids={duplicates}")
    load_dictionary(entries, args.ecdict)
    load_examples(entries, args.sentences)
    meaning = sum(1 for entry in entries if entry.get("meaning"))
    phonetic = sum(1 for entry in entries if entry.get("phonetic"))
    example = sum(1 for entry in entries if entry.get("example"))
    print(f"meaning={meaning} phonetic={phonetic} example={example}")
    print("NO_MEANING", [entry["word"] for entry in entries if not entry.get("meaning")])
    for word in ("a/an", "the", "abandon", "ability", "apple", "schedule", "present", "you (your, yours, yourself, yourselves)"):
        found = next((entry for entry in entries if entry["word"] == word), None)
        if found:
            print(
                "SAMPLE",
                found["word"],
                found["pos"],
                found["level"],
                found.get("meaning"),
                found.get("phonetic"),
                found.get("definition"),
                found.get("example"),
            )
    write_asset(entries, args.out)
    print(f"wrote {args.out} ({args.out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
