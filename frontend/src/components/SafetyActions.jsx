import React from "react";

function actionsForVerdict(verdict) {
  if (verdict === "Safe") {
    return [
      { label: "Safe to Proceed", tone: "ok" },
      { label: "Keep Monitoring", tone: "neutral" }
    ];
  }
  if (verdict === "LowRisk") {
    return [
      { label: "Proceed Carefully", tone: "ok" },
      { label: "Double-check Links", tone: "warn" }
    ];
  }
  if (verdict === "Suspicious") {
    return [
      { label: "Avoid Clicking", tone: "warn" },
      { label: "Verify Sender Independently", tone: "warn" },
      { label: "Report Phishing", tone: "neutral" }
    ];
  }
  return [
    { label: "Avoid Clicking", tone: "bad" },
    { label: "Report Phishing", tone: "bad" },
    { label: "Block Sender", tone: "warn" },
    { label: "Delete Message", tone: "warn" },
    { label: "Verify Sender Independently", tone: "neutral" }
  ];
}

function toneClass(tone) {
  if (tone === "ok") return "btn btn-ok";
  if (tone === "warn") return "btn btn-warn";
  if (tone === "bad") return "btn btn-bad";
  return "btn btn-ghost";
}

export default function SafetyActions({ verdict, recommendedActions }) {
  const quick = actionsForVerdict(verdict);
  const list = Array.isArray(recommendedActions) ? recommendedActions : [];

  return (
    <section className="card">
      <div className="card-header">
        <div>
          <div className="card-title">Safety actions</div>
          <div className="muted">Recommended next steps</div>
        </div>
      </div>

      <div className="btn-row">
        {quick.map((a) => (
          <button key={a.label} type="button" className={toneClass(a.tone)} onClick={() => {}}>
            {a.label}
          </button>
        ))}
      </div>

      {list.length ? (
        <ul className="bullets">
          {list.map((a, idx) => (
            <li key={`${a}-${idx}`}>{a}</li>
          ))}
        </ul>
      ) : (
        <div className="muted">No actions returned.</div>
      )}
    </section>
  );
}

