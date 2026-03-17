import React from "react";
import { Link } from "react-router-dom";

const tips = [
  { title: "Verify the sender", desc: "Check the email address and domain carefully. Look for subtle misspellings." },
  { title: "Avoid urgency traps", desc: "Scammers pressure you to act immediately. Slow down and verify." },
  { title: "Never share OTP/passwords", desc: "Legitimate companies don’t ask for OTPs or passwords over email or chat." },
  { title: "Inspect links before clicking", desc: "Hover to preview. Use official websites instead of links in messages." }
];

export default function SecurityTipsPanel() {
  return (
    <section className="card">
      <div className="card-header">
        <div>
          <div className="card-title">Security Awareness</div>
          <div className="muted">Small habits that prevent big incidents</div>
        </div>
        <Link className="btn btn-ghost" to="/tips">
          View all tips
        </Link>
      </div>

      <div className="tips-grid">
        {tips.map((t) => (
          <div key={t.title} className="tip">
            <div className="tip-title">{t.title}</div>
            <div className="muted">{t.desc}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

