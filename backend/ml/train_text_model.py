import argparse
import json
import re
from dataclasses import asdict, dataclass

import numpy as np
import pandas as pd
from sklearn.linear_model import SGDClassifier
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split


TOKEN_RE = re.compile(r"[a-zA-Z0-9]+")


def tokens(s: str):
    return TOKEN_RE.findall((s or "").lower())


def hash32(s: str) -> int:
    # FNV-1a 32-bit (matches Java implementation below)
    h = 2166136261
    for ch in s.encode("utf-8", errors="ignore"):
        h ^= ch
        h = (h * 16777619) & 0xFFFFFFFF
    return h


def vectorize(text: str, n_features: int) -> np.ndarray:
    x = np.zeros(n_features, dtype=np.float32)
    for tok in tokens(text):
        idx = hash32(tok) % n_features
        x[idx] += 1.0
    # normalize a bit
    norm = np.linalg.norm(x)
    if norm > 0:
        x /= norm
    return x


@dataclass
class Weights:
    bias: float
    nFeatures: int
    weights: list[float]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", required=True, help="CSV with columns: text,label (label 1=spam/phish,0=ham)")
    ap.add_argument("--out", required=True, help="Output weights JSON path")
    ap.add_argument("--n-features", type=int, default=8192)
    ap.add_argument("--test-size", type=float, default=0.2)
    args = ap.parse_args()

    df = pd.read_csv(args.csv)
    if "text" not in df.columns or "label" not in df.columns:
        raise SystemExit("CSV must have columns: text,label")

    X = np.vstack([vectorize(t, args.n_features) for t in df["text"].astype(str).tolist()])
    y = df["label"].astype(int).to_numpy()

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=args.test_size, random_state=42, stratify=y if len(set(y)) > 1 else None
    )

    clf = SGDClassifier(loss="log_loss", max_iter=2000, tol=1e-4)
    clf.fit(X_train, y_train)

    preds = clf.predict(X_test)
    acc = accuracy_score(y_test, preds)
    print(f"accuracy={acc:.4f}")
    print(classification_report(y_test, preds, digits=4))

    w = Weights(bias=float(clf.intercept_[0]), nFeatures=int(args.n_features), weights=clf.coef_[0].astype(float).tolist())
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(asdict(w), f)
    print(f"wrote weights -> {args.out}")


if __name__ == "__main__":
    main()

