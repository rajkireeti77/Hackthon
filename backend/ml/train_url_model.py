import argparse
import json
from dataclasses import asdict, dataclass

import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report


RISKY_TLDS = {
    "zip",
    "mov",
    "click",
    "top",
    "xyz",
    "gq",
    "tk",
    "ml",
    "cf",
    "work",
    "life",
    "live",
    "rest",
    "country",
    "stream",
    "download",
    "kim",
    "men",
    "quest",
    "link",
}

SUSPICIOUS_KEYWORDS = ["login", "verify", "secure", "update", "account", "bank", "confirm", "password", "otp"]


def looks_like_ipv4(host: str) -> bool:
    parts = host.split(".")
    if len(parts) != 4:
        return False
    for p in parts:
        if not p.isdigit() or len(p) > 3:
            return False
        v = int(p)
        if v < 0 or v > 255:
            return False
    return True


def features(url: str):
    u = (url or "").strip()
    lower = u.lower()

    https = 1.0 if lower.startswith("https://") else 0.0
    has_at = 1.0 if "@" in u else 0.0

    # crude host extraction (enough for training)
    host = lower
    if "://" in host:
        host = host.split("://", 1)[1]
    host = host.split("/", 1)[0]
    host = host.split("?", 1)[0]
    host = host.split("#", 1)[0]

    has_punycode = 1.0 if "xn--" in host else 0.0
    has_ip_host = 1.0 if looks_like_ipv4(host) else 0.0
    dots = min(host.count("."), 10) / 10.0
    hyphens = min(host.count("-"), 10) / 10.0

    digits = sum(ch.isdigit() for ch in host)
    digits_ratio = 0.0 if len(host) == 0 else (digits / len(host))

    tld = host.rsplit(".", 1)[-1] if "." in host else ""
    risky_tld = 1.0 if tld in RISKY_TLDS else 0.0

    keyword_hits = sum(1 for k in SUSPICIOUS_KEYWORDS if k in lower)
    keyword_hits = min(keyword_hits, 6) / 6.0

    length = min(len(u), 200) / 200.0

    return [
        length,
        dots,
        hyphens,
        has_at,
        has_ip_host,
        has_punycode,
        digits_ratio,
        risky_tld,
        keyword_hits,
        https,
    ]


@dataclass
class Weights:
    bias: float
    wLength: float
    wDots: float
    wHyphens: float
    wHasAt: float
    wHasIpHost: float
    wHasPunycode: float
    wDigitsInHost: float
    wRiskyTld: float
    wKeywordHits: float
    wHttps: float


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", required=True, help="CSV with columns: url,label (label 1=phishing,0=benign)")
    ap.add_argument("--out", required=True, help="Output weights JSON path")
    ap.add_argument("--test-size", type=float, default=0.2)
    args = ap.parse_args()

    df = pd.read_csv(args.csv)
    if "url" not in df.columns or "label" not in df.columns:
        raise SystemExit("CSV must have columns: url,label")

    X = df["url"].astype(str).map(features).tolist()
    y = df["label"].astype(int).tolist()

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=args.test_size, random_state=42, stratify=y if len(set(y)) > 1 else None
    )

    clf = LogisticRegression(max_iter=2000)
    clf.fit(X_train, y_train)

    preds = clf.predict(X_test)
    acc = accuracy_score(y_test, preds)
    print(f"accuracy={acc:.4f}")
    print(classification_report(y_test, preds, digits=4))

    coef = clf.coef_[0].tolist()
    intercept = float(clf.intercept_[0])

    w = Weights(
        bias=intercept,
        wLength=coef[0],
        wDots=coef[1],
        wHyphens=coef[2],
        wHasAt=coef[3],
        wHasIpHost=coef[4],
        wHasPunycode=coef[5],
        wDigitsInHost=coef[6],
        wRiskyTld=coef[7],
        wKeywordHits=coef[8],
        wHttps=coef[9],
    )

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(asdict(w), f, indent=2)
    print(f"wrote weights -> {args.out}")


if __name__ == "__main__":
    main()

