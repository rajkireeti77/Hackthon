import React from "react";
import { scoreColor } from "../utils/format";

export default function RiskMeter({ score = 0 }) {
  const s = Math.max(0, Math.min(100, Number(score) || 0));
  const color = scoreColor(s);
  return (
    <div className="risk-meter">
      <div className="risk-top">
        <div className="risk-label">Risk score</div>
        <div className="risk-score" style={{ color }}>
          {s}/100
        </div>
      </div>
      <div className="progress">
        <div className="progress-bar" style={{ width: `${s}%`, background: color }} />
      </div>
      <div className="risk-legend muted">0–24 Safe • 25–49 Suspicious • 50–100 Malicious</div>
    </div>
  );
}

