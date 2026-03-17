import argparse
import csv
import os
import random
import re
import subprocess
import sys
from urllib.request import urlopen, Request
import gzip


def http_get_bytes(url: str) -> bytes:
    """
    Best-effort downloader for Windows environments.
    Tries curl.exe first (more reliable behind some networks), falls back to urllib.
    """
    curl = "curl.exe" if sys.platform.startswith("win") else "curl"
    try:
        p = subprocess.run(
            [curl, "-L", "--retry", "3", "--retry-delay", "2", "--connect-timeout", "20", "--max-time", "120", url],
            capture_output=True,
            check=True,
        )
        if p.stdout:
            return p.stdout
    except Exception:
        pass

    req = Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urlopen(req, timeout=120) as r:
        return r.read()


def normalize_url(u: str) -> str | None:
    u = (u or "").strip()
    if not u:
        return None
    # keep only http(s) urls
    if not (u.startswith("http://") or u.startswith("https://")):
        return None
    return u


def download_url_dataset(out_csv: str, max_phishing: int = 60000, max_benign: int = 60000):
    """
    Creates CSV with columns: url,label (1=phishing,0=benign)

    Sources (public, no auth):
    - Phishing URLs: OpenPhish feed (small), plus a maintained phishing URL list on GitHub.
    - Benign URLs: Tranco top-1m list.
    """
    phishing_sources = [
        # community-maintained phishing URL list
        "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-links-ACTIVE.txt",
        # fallback: another common phishing list (URLs/domains)
        "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-domains-ACTIVE.txt",
    ]

    # Benign: CrUX top sites list (gzipped CSV of origins)
    benign_gz = "https://raw.githubusercontent.com/zakird/crux-top-lists/main/data/global/current.csv.gz"

    phishing_urls: list[str] = []
    for src in phishing_sources:
        try:
            data = http_get_bytes(src).decode("utf-8", errors="ignore")
        except Exception:
            continue
        for line in data.splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            # Some lists are domains only; convert to URL so our feature extractor can parse host.
            if line.startswith("http://") or line.startswith("https://"):
                u = normalize_url(line)
                if u:
                    phishing_urls.append(u)
            else:
                # treat as domain
                if re.match(r"^[a-zA-Z0-9.-]+$", line):
                    phishing_urls.append("http://" + line)
        if len(phishing_urls) >= max_phishing:
            break

    phishing_urls = list(dict.fromkeys(phishing_urls))[:max_phishing]

    # Benign: CrUX (origins, already include scheme)
    gzbytes = http_get_bytes(benign_gz)
    content = gzip.decompress(gzbytes).decode("utf-8", errors="ignore")
    benign_urls: list[str] = []
    for line in content.splitlines():
        line = line.strip()
        if not line:
            continue
        # format: origin,rank_bucket (we only need origin)
        parts = line.split(",")
        origin = parts[0].strip()
        u = normalize_url(origin)
        if u:
            benign_urls.append(u)
        if len(benign_urls) >= max_benign:
            break

    os.makedirs(os.path.dirname(out_csv), exist_ok=True)
    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["url", "label"])
        for u in phishing_urls:
            w.writerow([u, 1])
        for u in benign_urls:
            w.writerow([u, 0])

    print(f"Wrote URL dataset: {out_csv} phishing={len(phishing_urls)} benign={len(benign_urls)}")


def download_sms_dataset(out_csv: str):
    """
    Creates CSV with columns: text,label (1=spam,0=ham)
    Source: UCI SMS Spam Collection (public).
    """
    # Mirror on GitHub (raw text) to avoid UCI TLS/network issues.
    txt_url = "https://raw.githubusercontent.com/fenago/datasets/main/SMSSpamCollection.txt"
    raw = http_get_bytes(txt_url).decode("utf-8", errors="ignore")

    rows = []
    for line in raw.splitlines():
        if not line.strip():
            continue
        label, text = line.split("\t", 1)
        label = label.strip().lower()
        y = 1 if label == "spam" else 0
        rows.append((text.strip(), y))

    os.makedirs(os.path.dirname(out_csv), exist_ok=True)
    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["text", "label"])
        for text, y in rows:
            w.writerow([text, y])

    print(f"Wrote SMS dataset: {out_csv} rows={len(rows)}")


def build_file_dataset(out_csv: str, rows_per_family: int = 350):
    """
    Creates CSV with columns: file_name,content_type,file_size_bytes,label

    This dataset is synthetic but grounded in common malware delivery patterns:
    - malicious/scriptable files: exe, dll, js, vbs, ps1, bat, scr, jar, apk, msi, lnk, iso, zip, docm, xlsm
    - benign files: images, office docs, plain text, pdf, audio/video, source code, csv

    We use a synthetic dataset here because openly redistributable malware-file corpora
    are scarce and often unsafe or license-restricted for hackathon projects.
    """
    rng = random.Random(42)

    suspicious_bases = [
        "invoice",
        "payment",
        "remittance",
        "salary",
        "courier",
        "statement",
        "account_update",
        "security_patch",
        "reset",
        "verification",
        "bank_notice",
        "urgent_document",
        "scan_copy",
        "resume",
        "wire_transfer",
    ]
    benign_bases = [
        "holiday_photo",
        "meeting_notes",
        "project_plan",
        "design_mockup",
        "family_video",
        "budget_2026",
        "report_final",
        "lecture_notes",
        "data_export",
        "product_specs",
        "profile_picture",
        "roadmap",
        "team_directory",
        "release_notes",
        "travel_itinerary",
    ]

    malicious_profiles = [
        ("exe", "application/x-msdownload", (90_000, 5_000_000)),
        ("dll", "application/x-msdownload", (40_000, 2_000_000)),
        ("scr", "application/x-msdownload", (60_000, 3_500_000)),
        ("bat", "application/x-bat", (800, 30_000)),
        ("cmd", "application/x-bat", (700, 30_000)),
        ("ps1", "text/plain", (800, 40_000)),
        ("js", "application/javascript", (900, 80_000)),
        ("vbs", "text/plain", (700, 35_000)),
        ("hta", "application/hta", (1_000, 40_000)),
        ("jar", "application/java-archive", (30_000, 12_000_000)),
        ("apk", "application/vnd.android.package-archive", (2_000_000, 120_000_000)),
        ("msi", "application/x-msi", (80_000, 25_000_000)),
        ("lnk", "application/x-ms-shortcut", (1_000, 8_000)),
        ("iso", "application/x-iso9660-image", (150_000_000, 4_000_000_000)),
        ("zip", "application/zip", (30_000, 8_000_000)),
        ("docm", "application/vnd.ms-word.document.macroEnabled.12", (30_000, 4_000_000)),
        ("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12", (20_000, 3_000_000)),
    ]
    benign_profiles = [
        ("pdf", "application/pdf", (15_000, 40_000_000)),
        ("png", "image/png", (8_000, 12_000_000)),
        ("jpg", "image/jpeg", (6_000, 16_000_000)),
        ("jpeg", "image/jpeg", (6_000, 16_000_000)),
        ("txt", "text/plain", (50, 2_000_000)),
        ("csv", "text/csv", (100, 10_000_000)),
        ("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", (12_000, 8_000_000)),
        ("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", (12_000, 8_000_000)),
        ("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", (12_000, 25_000_000)),
        ("mp3", "audio/mpeg", (60_000, 15_000_000)),
        ("mp4", "video/mp4", (1_000_000, 300_000_000)),
        ("json", "application/json", (30, 2_000_000)),
        ("html", "text/html", (200, 1_000_000)),
        ("py", "text/x-python", (50, 800_000)),
        ("java", "text/x-java-source", (50, 1_200_000)),
    ]

    def random_name(base_names, ext, suspicious):
      base = rng.choice(base_names)
      suffix = rng.choice(["", "_copy", "_final", "_2026", "_new", "_secure", "_review"])
      if suspicious and rng.random() < 0.28:
        bait = rng.choice(["pdf", "doc", "xls", "jpg", "png", "txt"])
        return f"{base}{suffix}.{bait}.{ext}"
      return f"{base}{suffix}.{ext}"

    rows = []
    for ext, content_type, size_range in malicious_profiles:
      for _ in range(rows_per_family):
        rows.append(
            {
                "file_name": random_name(suspicious_bases, ext, True),
                "content_type": content_type,
                "file_size_bytes": rng.randint(*size_range),
                "label": 1,
            }
        )
    for ext, content_type, size_range in benign_profiles:
      for _ in range(rows_per_family):
        rows.append(
            {
                "file_name": random_name(benign_bases, ext, False),
                "content_type": content_type,
                "file_size_bytes": rng.randint(*size_range),
                "label": 0,
            }
        )

    rng.shuffle(rows)

    os.makedirs(os.path.dirname(out_csv), exist_ok=True)
    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["file_name", "content_type", "file_size_bytes", "label"])
        w.writeheader()
        w.writerows(rows)

    malicious_count = sum(1 for row in rows if row["label"] == 1)
    benign_count = len(rows) - malicious_count
    print(f"Wrote file dataset: {out_csv} malicious={malicious_count} benign={benign_count}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default="data", help="Output directory under backend/ml/")
    ap.add_argument("--max-phishing", type=int, default=60000)
    ap.add_argument("--max-benign", type=int, default=60000)
    args = ap.parse_args()

    out_dir = args.out_dir
    url_csv = os.path.join(out_dir, "urls.csv")
    sms_csv = os.path.join(out_dir, "sms.csv")
    file_csv = os.path.join(out_dir, "files.csv")

    download_url_dataset(url_csv, max_phishing=args.max_phishing, max_benign=args.max_benign)
    download_sms_dataset(sms_csv)
    build_file_dataset(file_csv)


if __name__ == "__main__":
    main()

