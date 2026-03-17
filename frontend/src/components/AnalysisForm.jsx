import React, { useMemo, useState } from "react";

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

function isLikelyUrl(value) {
  const v = String(value || "").trim();
  if (!v) return true;
  try {
    const u = new URL(v);
    return u.protocol === "http:" || u.protocol === "https:";
  } catch {
    return false;
  }
}

function formatBytes(bytes) {
  const value = Number(bytes) || 0;
  if (!value) return "0 B";
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function toHex(buffer) {
  return Array.from(new Uint8Array(buffer))
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("");
}

function toBase64(bytes) {
  let binary = "";
  bytes.forEach((value) => {
    binary += String.fromCharCode(value);
  });
  return window.btoa(binary);
}

async function createFilePayload(file) {
  const arrayBuffer = await file.arrayBuffer();
  const hashBuffer = await window.crypto.subtle.digest("SHA-256", arrayBuffer);
  const sample = new Uint8Array(arrayBuffer.slice(0, 4096));

  return {
    fileName: file.name,
    fileContentType: file.type || "",
    fileSizeBytes: file.size,
    fileSha256: toHex(hashBuffer),
    fileSampleBase64: sample.length ? toBase64(sample) : ""
  };
}

export default function AnalysisForm({ onAnalyze, loading }) {
  const [textContent, setTextContent] = useState("");
  const [url, setUrl] = useState("");
  const [file, setFile] = useState(null);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [touched, setTouched] = useState(false);

  const errors = useMemo(() => {
    const e = {};
    const t = textContent.trim();
    const u = url.trim();

    if (!t && !u && !file) e.form = "Please enter text, a URL, and/or attach a file.";
    if (u && !isLikelyUrl(u)) e.url = "URL must start with http:// or https://";
    if (t.length > 10000) e.textContent = "Text is too long (max 10,000 characters).";
    if (u.length > 2048) e.url = "URL is too long (max 2048 characters).";
    if (file && file.size > MAX_FILE_SIZE_BYTES) e.file = "File is too large (max 10 MB).";
    return e;
  }, [file, textContent, url]);

  const canSubmit = Object.keys(errors).length === 0;

  async function submit(e) {
    e.preventDefault();
    setTouched(true);
    if (!canSubmit || loading) return;
    const payload = {
      textContent: textContent.trim(),
      url: url.trim()
    };
    if (file) {
      Object.assign(payload, await createFilePayload(file));
    }
    await onAnalyze(payload);
  }

  return (
    <section className="card">
      <div className="card-header">
        <div>
          <div className="card-title">Analyze content</div>
          <div className="muted">Paste an email/chat message and/or a URL</div>
        </div>
      </div>

      <form onSubmit={submit} className="form">
        <label className="field">
          <div className="field-label">Email / message text</div>
          <textarea
            className="input textarea"
            placeholder="Example: 'Your bank account will be blocked. Click immediately to verify your account.'"
            value={textContent}
            onChange={(e) => setTextContent(e.target.value)}
            rows={7}
          />
          {touched && errors.textContent ? <div className="field-error">{errors.textContent}</div> : null}
        </label>

        <label className="field">
          <div className="field-label">URL (optional)</div>
          <input
            className="input"
            placeholder="https://example.com"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
          />
          {touched && errors.url ? <div className="field-error">{errors.url}</div> : null}
        </label>

        <label className="field">
          <div className="field-label">Attachment (optional)</div>
          <input
            key={fileInputKey}
            className="input"
            type="file"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
          {file ? (
            <div className="muted">
              {file.name} • {formatBytes(file.size)} • {file.type || "unknown type"}
            </div>
          ) : null}
          {touched && errors.file ? <div className="field-error">{errors.file}</div> : null}
        </label>

        {touched && errors.form ? <div className="alert alert-warn">{errors.form}</div> : null}

        <div className="actions">
          <button className="btn btn-primary" type="submit" disabled={!canSubmit || loading}>
            {loading ? "Analyzing..." : "Analyze"}
          </button>
          <button
            className="btn btn-ghost"
            type="button"
            disabled={loading}
            onClick={() => {
              setTextContent("");
              setUrl("");
              setFile(null);
              setFileInputKey((value) => value + 1);
              setTouched(false);
            }}
          >
            Clear
          </button>
        </div>
      </form>
    </section>
  );
}

