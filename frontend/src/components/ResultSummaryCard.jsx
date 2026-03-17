import React from "react";
import RiskMeter from "./RiskMeter";
import { formatVerdict } from "../utils/format";

function badgeClass(verdict) {
  if (verdict === "Safe") return "badge badge-safe";
  if (verdict === "LowRisk") return "badge badge-low";
  if (verdict === "Suspicious") return "badge badge-warn";
  return "badge badge-bad";
}

export default function ResultSummaryCard({ result }) {
  if (!result) return null;
  return (
    <section className="card">
      <div className="card-header">
        <div>
          <div className="card-title">Scan Result</div>
          <div className="muted">Explainable output you can act on</div>
        </div>
        <div className={badgeClass(result.verdict)}>{formatVerdict(result.verdict)}</div>
      </div>

      <div className="grid-2">
        <div className="stat">
          <div className="stat-k">Severity</div>
          <div className="stat-v">{result.severity}</div>
        </div>
        <div className="stat">
          <div className="stat-k">Confidence</div>
          <div className="stat-v">{Math.round((Number(result.confidence) || 0) * 100)}%</div>
        </div>
      </div>

      {result.fileName ? (
        <div className="stat" style={{ marginTop: 12 }}>
          <div className="stat-k">Attachment scanned</div>
          <div className="stat-v">{result.fileName}</div>
          <div className="muted">
            {result.fileContentType || "unknown type"}
            {result.fileSizeBytes ? ` • ${Math.round(result.fileSizeBytes / 1024)} KB` : ""}
          </div>
        </div>
      ) : null}

      <RiskMeter score={result.riskScore} />

      <div className="divider" />
      <div className="card-title">Summary</div>
      <p className="summary">{result.summary || "No summary returned."}</p>
    </section>
  );
}

