import React from "react";
import { Link } from "react-router-dom";
import { formatDate, formatVerdict, scoreColor } from "../utils/format";

function verdictBadge(v) {
  if (v === "Safe") return "badge badge-safe";
  if (v === "LowRisk") return "badge badge-low";
  if (v === "Suspicious") return "badge badge-warn";
  return "badge badge-bad";
}

export default function RecentScansTable({ items }) {
  if (!items || items.length === 0) return null;

  return (
    <section className="card">
      <div className="card-header">
        <div>
          <div className="card-title">Recent scans</div>
          <div className="muted">Your latest 20 analyses</div>
        </div>
      </div>

      <div className="table">
        <div className="row head">
          <div>ID</div>
          <div>Type</div>
          <div>Verdict</div>
          <div>Risk</div>
          <div>Date</div>
          <div />
        </div>
        {items.map((r) => (
          <div key={r.id} className="row">
            <div className="mono">#{r.id}</div>
            <div className="mono">{r.inputType}</div>
            <div>
              <span className={verdictBadge(r.verdict)}>{formatVerdict(r.verdict)}</span>
            </div>
            <div className="mono" style={{ color: scoreColor(r.riskScore) }}>
              {r.riskScore}
            </div>
            <div className="mono">{formatDate(r.createdAt)}</div>
            <div className="right">
              <Link className="btn btn-small" to={`/scans/${r.id}`}>
                Open
              </Link>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

