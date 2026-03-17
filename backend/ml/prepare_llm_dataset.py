import argparse
import json
import math
import random
from pathlib import Path

import pandas as pd


SYSTEM_PROMPT = (
    "You are PhishGuard AI, a cybersecurity assistant that classifies text, URLs, and file metadata. "
    "Return only strict JSON with keys verdict, riskScore, severity, confidence, summary, redFlags, recommendedActions."
)


def clamp(value: int, low: int = 0, high: int = 100) -> int:
    return max(low, min(high, value))


def severity_for(score: int) -> str:
    if score >= 75:
        return "Critical"
    if score >= 60:
        return "High"
    if score >= 35:
        return "Medium"
    return "Low"


def verdict_for(score: int) -> str:
    if score >= 60:
        return "Malicious"
    if score >= 35:
        return "Suspicious"
    return "Safe"


def build_response(score: int, summary: str, red_flags: list[dict], actions: list[str]) -> str:
    payload = {
        "verdict": verdict_for(score),
        "riskScore": score,
        "severity": severity_for(score),
        "confidence": round(0.72 if score >= 60 else (0.64 if score >= 35 else 0.58), 2),
        "summary": summary,
        "redFlags": red_flags[:4],
        "recommendedActions": actions[:5],
    }
    return json.dumps(payload, ensure_ascii=True)


def text_example(text: str, label: int) -> dict:
    lower = (text or "").lower()
    score = 18 if label == 0 else 72
    flags = []
    if any(word in lower for word in ["urgent", "immediately", "act now", "final notice"]):
      score += 8
      flags.append({"type": "Urgency", "severity": "High", "description": "Uses pressure to force quick action."})
    if any(word in lower for word in ["password", "otp", "pin", "cvv", "bank"]):
      score += 10
      flags.append({"type": "Credential Theft", "severity": "Critical", "description": "Requests sensitive credentials or banking data."})
    if any(word in lower for word in ["click", "tap", "verify", "reset", "login"]):
      score += 6
      flags.append({"type": "Suspicious Action", "severity": "Medium", "description": "Pushes the victim to click or verify quickly."})
    if label == 0 and not flags:
      flags.append({"type": "Benign Language", "severity": "Low", "description": "No strong phishing language detected."})
    score = clamp(score)
    summary = (
        "This message uses phishing-style language and should be treated as suspicious."
        if label == 1
        else "This message looks mostly benign and does not show strong phishing indicators."
    )
    return {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"Analyze this message:\ntextContent={text}\nurl=\nfileContext=\nsafeBrowsingFlagged=false\nthreatTypes=[]"},
            {"role": "assistant", "content": build_response(score, summary, flags, [
                "Do not click unknown links",
                "Verify the sender via official channels",
                "Avoid sharing OTPs or passwords",
                "Report the message if it looks suspicious",
            ])},
        ]
    }


def url_example(url: str, label: int) -> dict:
    lower = (url or "").lower()
    score = 15 if label == 0 else 78
    flags = []
    if "@" in lower:
      score += 12
      flags.append({"type": "Suspicious URL", "severity": "Critical", "description": "Contains an @ symbol, which is commonly abused in phishing URLs."})
    if any(token in lower for token in ["login", "verify", "secure", "account", "update", "bank"]):
      score += 7
      flags.append({"type": "Credential Harvesting", "severity": "High", "description": "The URL contains phishing-related keywords."})
    if lower.startswith("http://"):
      score += 5
      flags.append({"type": "No HTTPS", "severity": "Medium", "description": "The URL does not use HTTPS."})
    if any(lower.endswith("." + tld) or f".{tld}/" in lower for tld in ["zip", "mov", "click", "xyz", "top", "link"]):
      score += 8
      flags.append({"type": "Risky TLD", "severity": "High", "description": "The URL uses a TLD often associated with abuse."})
    if label == 0 and not flags:
      flags.append({"type": "Normal URL Shape", "severity": "Low", "description": "The URL structure looks typical for a benign site."})
    score = clamp(score)
    summary = (
        "This URL shows phishing-style structure or delivery indicators."
        if label == 1
        else "This URL looks relatively normal and does not expose strong phishing indicators."
    )
    return {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"Analyze this message:\ntextContent=\nurl={url}\nfileContext=\nsafeBrowsingFlagged=false\nthreatTypes=[]"},
            {"role": "assistant", "content": build_response(score, summary, flags, [
                "Avoid opening unknown links",
                "Type important websites manually instead of trusting message links",
                "Use Safe Browsing or reputation tools before visiting",
                "Report malicious links to your security team",
            ])},
        ]
    }


def file_example(file_name: str, content_type: str, file_size_bytes: int, label: int) -> dict:
    lower = (file_name or "").lower()
    score = 16 if label == 0 else 80
    flags = []
    ext = lower.rsplit(".", 1)[-1] if "." in lower else ""
    if lower.count(".") >= 2:
      score += 10
      flags.append({"type": "Double Extension", "severity": "Critical", "description": "The filename hides an executable behind another extension."})
    if ext in {"exe", "dll", "scr", "bat", "cmd", "ps1", "js", "vbs", "jar", "apk", "msi", "lnk"}:
      score += 12
      flags.append({"type": "Executable Attachment", "severity": "Critical", "description": "The file uses an executable or scriptable format."})
    if ext in {"docm", "xlsm", "pptm"}:
      score += 10
      flags.append({"type": "Macro Document", "severity": "High", "description": "The file is macro-enabled and may run embedded code."})
    if ext in {"zip", "iso", "rar", "7z"}:
      score += 7
      flags.append({"type": "Archive Delivery", "severity": "Medium", "description": "Archives are commonly used to hide malware payloads."})
    if file_size_bytes and math.log10(file_size_bytes + 1) > 7.5 and label == 1:
      score += 4
    if label == 0 and not flags:
      flags.append({"type": "Benign File Type", "severity": "Low", "description": "The attachment format appears routine."})
    score = clamp(score)
    summary = (
        "This attachment looks risky and may be used to deliver malware."
        if label == 1
        else "This attachment metadata looks routine and does not show strong malware indicators."
    )
    file_context = f"fileName={file_name}, contentType={content_type}, sizeBytes={file_size_bytes}"
    return {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"Analyze this message:\ntextContent=\nurl=\nfileContext={file_context}\nsafeBrowsingFlagged=false\nthreatTypes=[]"},
            {"role": "assistant", "content": build_response(score, summary, flags, [
                "Do not open unexpected attachments",
                "Scan the file with antivirus or sandbox tooling",
                "Verify the sender before opening the file",
                "Block or quarantine suspicious attachments",
            ])},
        ]
    }


def main():
    base_dir = Path(__file__).resolve().parent
    ap = argparse.ArgumentParser()
    ap.add_argument("--sms-csv", default=str(base_dir / "data" / "sms.csv"))
    ap.add_argument("--urls-csv", default=str(base_dir / "data" / "urls.csv"))
    ap.add_argument("--files-csv", default=str(base_dir / "data" / "files.csv"))
    ap.add_argument("--out-dir", default=str(base_dir / "llm-data"))
    ap.add_argument("--max-sms", type=int, default=4000)
    ap.add_argument("--max-urls", type=int, default=12000)
    ap.add_argument("--max-files", type=int, default=6000)
    args = ap.parse_args()

    rng = random.Random(42)
    examples = []

    sms_source = Path(args.sms_csv)
    urls_source = Path(args.urls_csv)
    files_source = Path(args.files_csv)
    sms_raw = pd.read_csv(sms_source)
    urls_raw = pd.read_csv(urls_source)
    files_raw = pd.read_csv(files_source)

    sms_df = sms_raw.sample(n=min(args.max_sms, len(sms_raw)), random_state=42)
    for row in sms_df.itertuples(index=False):
      examples.append(text_example(str(row.text), int(row.label)))

    urls_df = urls_raw.sample(n=min(args.max_urls, len(urls_raw)), random_state=42)
    for row in urls_df.itertuples(index=False):
      examples.append(url_example(str(row.url), int(row.label)))

    files_df = files_raw.sample(n=min(args.max_files, len(files_raw)), random_state=42)
    for row in files_df.itertuples(index=False):
      examples.append(file_example(str(row.file_name), str(row.content_type), int(row.file_size_bytes), int(row.label)))

    rng.shuffle(examples)
    split = int(len(examples) * 0.9)
    train_rows = examples[:split]
    val_rows = examples[split:]

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    train_path = out_dir / "train.jsonl"
    val_path = out_dir / "valid.jsonl"

    with train_path.open("w", encoding="utf-8") as f:
      for row in train_rows:
        f.write(json.dumps(row, ensure_ascii=True) + "\n")

    with val_path.open("w", encoding="utf-8") as f:
      for row in val_rows:
        f.write(json.dumps(row, ensure_ascii=True) + "\n")

    print(f"Wrote {len(train_rows)} training rows -> {train_path}")
    print(f"Wrote {len(val_rows)} validation rows -> {val_path}")


if __name__ == "__main__":
    main()
