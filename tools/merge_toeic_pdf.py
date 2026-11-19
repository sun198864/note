#!/usr/bin/env python3
"""Merge a 刷刷鍋-style TOEIC PDF into app/src/main/assets/toeic.json.

Existing toeic entries are kept. PDF words already covered by toeic are skipped.
Words already in vocab.json are appended as {"word": ...} only.
Brand-new words are appended with meaning and examples from the PDF.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import unicodedata
from pathlib import Path

from pypdf import PdfReader

try:
    import zhconv
except ImportError:  # pragma: no cover
    zhconv = None

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_TOEIC = ROOT / "app/src/main/assets/toeic.json"
DEFAULT_VOCAB = ROOT / "app/src/main/assets/vocab.json"

TYPOS = {
    "aprove": "approve",
    "nconvenience": "inconvenience",
    "ompulsory": "compulsory",
}

LISTED_FORMS = re.compile(r"^(\S+)\s*\((.+)\)$")
SUFFIX_FORM = re.compile(r"^([^(]+)\(([^)]+)\)$")
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
HEADER_RE = re.compile(
    r"^([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ'\-/]*)\s*[（(]\s*([^）)]+?)\s*[）)]\s*(.+)$"
)
POS_CLEAN_RE = re.compile(r"\s+")
EXTRA_POS_RE = re.compile(r"[（(]\s*((?:n|v|adj|adv|prep|conj)\.?)\s*[）)]", re.I)


def fold(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text.replace("’", "'").replace("‘", "'"))
    return "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn").lower()


def to_traditional(text: str) -> str:
    if not text or zhconv is None:
        return text
    return zhconv.convert(text, "zh-tw")


def headword_forms(word: str) -> list[str]:
    forms: list[str] = []
    for piece in word.split("/"):
        part = piece.strip()
        if not part:
            continue
        listed = LISTED_FORMS.fullmatch(part)
        if listed and "," in listed.group(2):
            forms.append(listed.group(1))
            forms.extend(item.strip() for item in listed.group(2).split(",") if item.strip())
            continue
        suffix = SUFFIX_FORM.fullmatch(part)
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


def densify(line: str) -> str:
    line = line.replace("\u00a0", " ").strip()
    line = re.sub(r"(?<=[\u4e00-\u9fff])\s+(?=[\u4e00-\u9fff])", "", line)
    line = re.sub(
        r"\b([A-Za-zÀ-ÿ])(?:\s+([A-Za-zÀ-ÿ]))+\b",
        lambda match: match.group(0).replace(" ", ""),
        line,
    )
    line = re.sub(r"([（(])\s+", r"\1", line)
    line = re.sub(r"\s+([）)])", r"\1", line)
    line = re.sub(r"\b([A-Za-z]+)\s+\.", r"\1.", line)
    return line.strip()


def normalize_pos(raw: str, meaning: str) -> str:
    pos = POS_CLEAN_RE.sub("", raw).lower()
    if not pos.endswith("."):
        pos += "."
    extras: list[str] = []
    for match in EXTRA_POS_RE.finditer(meaning):
        tag = match.group(1).lower().rstrip(".") + "."
        if tag != pos and tag not in extras:
            extras.append(tag)
    if extras:
        return "/".join([pos, *extras])
    return pos


def format_example(text: str) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return ""
    if re.match(r"^\d+\.\s*", text):
        return text
    return f"1. {text}"


def join_soft_wrap(parts: list[str]) -> str:
    if not parts:
        return ""
    out = parts[0].rstrip()
    for part in parts[1:]:
        part = part.strip()
        if out.endswith("-") and part and part[0].islower():
            out = out[:-1] + part
        else:
            out = f"{out} {part}"
    return out.strip()


def is_cjk_line(line: str) -> bool:
    return CJK_RE.search(line) is not None


def parse_pdf(path: Path) -> list[dict]:
    reader = PdfReader(str(path))
    entries: list[dict] = []
    seen: set[str] = set()
    for page in reader.pages:
        text = page.extract_text() or ""
        lines = [densify(line) for line in text.splitlines()]
        lines = [line for line in lines if line]
        index = 0
        while index < len(lines):
            match = HEADER_RE.match(lines[index])
            if not match:
                index += 1
                continue
            raw_word = match.group(1).strip()
            raw_word = TYPOS.get(fold(raw_word), raw_word)
            if any(ord(ch) > 127 for ch in raw_word):
                raw_word = fold(raw_word)
            pos_raw = match.group(2).strip()
            meaning = to_traditional(densify(match.group(3).strip()))
            index += 1
            eng_parts: list[str] = []
            while (
                index < len(lines)
                and not HEADER_RE.match(lines[index])
                and not is_cjk_line(lines[index])
            ):
                eng_parts.append(lines[index])
                index += 1
            zh_parts: list[str] = []
            while (
                index < len(lines)
                and not HEADER_RE.match(lines[index])
                and is_cjk_line(lines[index])
            ):
                zh_parts.append(lines[index])
                index += 1
            key = fold(raw_word)
            if key in seen:
                continue
            seen.add(key)
            entries.append(
                {
                    "word": raw_word,
                    "pos": normalize_pos(pos_raw, meaning),
                    "meaning": meaning,
                    "example": format_example(join_soft_wrap(eng_parts)),
                    "exampleZh": format_example(to_traditional(join_soft_wrap(zh_parts))),
                }
            )
    return entries


def bank_forms(entries: list[dict]) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for entry in entries:
        word = entry["word"]
        for form in headword_forms(word):
            mapping.setdefault(fold(form), word)
    return mapping


def make_id(word: str, pos: str) -> str:
    return hashlib.sha1(f"{word}\t{pos}\ttoeic".encode("utf-8")).hexdigest()[:12]


def merge(pdf_entries: list[dict], toeic: list[dict], vocab: list[dict]) -> tuple[list[dict], dict]:
    toeic_forms = bank_forms(toeic)
    vocab_forms = bank_forms(vocab)
    word_only_added: list[str] = []
    full_added: list[str] = []
    skipped = 0
    for entry in pdf_entries:
        key = fold(entry["word"])
        if key in toeic_forms:
            skipped += 1
            continue
        if key in vocab_forms:
            canonical = vocab_forms[key]
            toeic.append({"word": canonical})
            toeic_forms[key] = canonical
            word_only_added.append(canonical)
            continue
        # Keep field order close to existing full toeic rows.
        toeic.append(
            {
                "word": entry["word"],
                "pos": entry["pos"],
                "meaning": entry["meaning"],
                "phonetic": "",
                "definition": "",
                "example": entry["example"],
                "exampleZh": entry["exampleZh"],
                "id": make_id(entry["word"], entry["pos"]),
            }
        )
        for form in headword_forms(entry["word"]):
            toeic_forms.setdefault(fold(form), entry["word"])
        full_added.append(entry["word"])
    stats = {
        "pdf_parsed": len(pdf_entries),
        "skipped_existing": skipped,
        "word_only_added": len(word_only_added),
        "full_added": len(full_added),
        "toeic_total": len(toeic),
        "sample_word_only": word_only_added[:20],
        "sample_full": full_added[:20],
        "missing_examples": [
            entry["word"]
            for entry in pdf_entries
            if not entry["example"] or not entry["exampleZh"]
        ],
    }
    return toeic, stats


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pdf", type=Path, help="Path to the TOEIC vocabulary PDF")
    parser.add_argument("--toeic", type=Path, default=DEFAULT_TOEIC)
    parser.add_argument("--vocab", type=Path, default=DEFAULT_VOCAB)
    args = parser.parse_args()

    pdf_entries = parse_pdf(args.pdf)
    toeic = json.loads(args.toeic.read_text(encoding="utf-8"))
    vocab = json.loads(args.vocab.read_text(encoding="utf-8"))
    merged, stats = merge(pdf_entries, toeic, vocab)
    args.toeic.write_text(
        json.dumps(merged, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(stats, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
