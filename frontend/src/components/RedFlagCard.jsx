import React from "react";

function sevClass(sev) {
  const s = String(sev || "").toLowerCase();
  if (s === "low") return "pill pill-low";
  if (s === "medium") return "pill pill-medium";
  if (s === "high") return "pill pill-high";
  return "pill pill-critical";
}

export default function RedFlagCard({ flag }) {
  if (!flag) return null;
  return (
    <div className="flag">
      <div className="flag-top">
        <div className="flag-type">{flag.type}</div>
        <span className={sevClass(flag.severity)}>{flag.severity}</span>
      </div>
      <div className="muted">{flag.description}</div>
    </div>
  );
}

