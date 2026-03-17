import React from "react";

const tips = [
  {
    title: "Verify sender emails",
    text: "Check the full email address and domain. Scammers often use lookalike domains."
  },
  {
    title: "Avoid clicking unknown links",
    text: "Use official websites or bookmarks. Don’t trust links in urgent messages."
  },
  {
    title: "Never share OTP/password",
    text: "No bank or company should ask for OTPs, passwords, or PINs via email or chat."
  },
  {
    title: "Check domain spelling",
    text: "Look for swapped letters (rn vs m), extra hyphens, or unusual top-level domains."
  },
  {
    title: "Beware urgency or fear tactics",
    text: "Threats like “account will be closed” are designed to make you act without thinking."
  },
  {
    title: "Verify independently",
    text: "Call official support numbers or use the official app/site to confirm requests."
  }
];

export default function SecurityTipsPage() {
  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h2>Security Tips</h2>
          <div className="muted">Practical habits to reduce phishing risk</div>
        </div>
      </div>

      <section className="card">
        <div className="tips-grid">
          {tips.map((t) => (
            <div key={t.title} className="tip">
              <div className="tip-title">{t.title}</div>
              <div className="muted">{t.text}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="card">
        <div className="card-title">If you suspect phishing</div>
        <ul className="bullets">
          <li>Do not click links or download attachments.</li>
          <li>Do not share credentials, OTPs, or personal data.</li>
          <li>Verify the request using official channels.</li>
          <li>Report the message to your email provider or organization.</li>
          <li>Block the sender and delete the message if confirmed malicious.</li>
        </ul>
      </section>
    </div>
  );
}

