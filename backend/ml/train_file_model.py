import argparse
import json
import math
from dataclasses import asdict, dataclass

import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split


EXECUTABLE_EXTENSIONS = {
    "exe",
    "dll",
    "scr",
    "bat",
    "cmd",
    "ps1",
    "js",
    "vbs",
    "vbe",
    "hta",
    "jar",
    "apk",
    "msi",
    "com",
    "reg",
    "cpl",
    "lnk",
}
ARCHIVE_EXTENSIONS = {"zip", "rar", "7z", "iso", "img"}
MACRO_EXTENSIONS = {"docm", "xlsm", "pptm"}
BENIGN_EXTENSIONS = {
    "pdf",
    "png",
    "jpg",
    "jpeg",
    "gif",
    "txt",
    "csv",
    "docx",
    "xlsx",
    "pptx",
    "mp3",
    "mp4",
    "json",
    "html",
    "py",
    "java",
}
SCRIPT_CONTENT_TYPES = {
    "application/javascript",
    "text/javascript",
    "text/plain",
    "application/x-bat",
    "application/hta",
}
ARCHIVE_CONTENT_TYPES = {
    "application/zip",
    "application/x-zip-compressed",
    "application/x-iso9660-image",
    "application/x-rar-compressed",
}


def normalize_extension(file_name: str) -> str:
    value = (file_name or "").strip().lower()
    if "." not in value:
        return ""
    return value.rsplit(".", 1)[-1]


def has_double_extension(file_name: str) -> bool:
    name = (file_name or "").strip().lower()
    parts = [p for p in name.split(".") if p]
    return len(parts) >= 3


def normalized_size(size_bytes: int) -> float:
    safe_size = max(int(size_bytes or 0), 0)
    return min(math.log10(safe_size + 1) / 9.0, 1.0)


def features(file_name: str, content_type: str, file_size_bytes: int):
    ext = normalize_extension(file_name)
    content = (content_type or "").strip().lower()

    executable = 1.0 if ext in EXECUTABLE_EXTENSIONS else 0.0
    archive = 1.0 if ext in ARCHIVE_EXTENSIONS else 0.0
    macro = 1.0 if ext in MACRO_EXTENSIONS else 0.0
    benign = 1.0 if ext in BENIGN_EXTENSIONS else 0.0
    double_extension = 1.0 if has_double_extension(file_name) else 0.0
    script_content = 1.0 if content in SCRIPT_CONTENT_TYPES else 0.0
    archive_content = 1.0 if content in ARCHIVE_CONTENT_TYPES else 0.0
    empty_content = 1.0 if not content else 0.0
    size_feature = normalized_size(file_size_bytes)

    return [
        executable,
        archive,
        macro,
        benign,
        double_extension,
        script_content,
        archive_content,
        empty_content,
        size_feature,
    ]


@dataclass
class Weights:
    bias: float
    wExecutableExt: float
    wArchiveExt: float
    wMacroExt: float
    wBenignExt: float
    wDoubleExtension: float
    wScriptContentType: float
    wArchiveContentType: float
    wEmptyContentType: float
    wSize: float


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", required=True, help="CSV with columns: file_name,content_type,file_size_bytes,label")
    ap.add_argument("--out", required=True, help="Output weights JSON path")
    ap.add_argument("--test-size", type=float, default=0.2)
    args = ap.parse_args()

    df = pd.read_csv(args.csv)
    required = {"file_name", "content_type", "file_size_bytes", "label"}
    if not required.issubset(df.columns):
        raise SystemExit("CSV must have columns: file_name,content_type,file_size_bytes,label")

    X = [
        features(row.file_name, row.content_type, row.file_size_bytes)
        for row in df[["file_name", "content_type", "file_size_bytes"]].itertuples(index=False)
    ]
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

    weights = Weights(
        bias=intercept,
        wExecutableExt=coef[0],
        wArchiveExt=coef[1],
        wMacroExt=coef[2],
        wBenignExt=coef[3],
        wDoubleExtension=coef[4],
        wScriptContentType=coef[5],
        wArchiveContentType=coef[6],
        wEmptyContentType=coef[7],
        wSize=coef[8],
    )

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(asdict(weights), f, indent=2)
    print(f"wrote weights -> {args.out}")


if __name__ == "__main__":
    main()
