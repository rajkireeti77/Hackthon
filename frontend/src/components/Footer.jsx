import React from "react";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <span className="muted">© {new Date().getFullYear()} PhishGuard AI</span>
        <span className="muted">Stay alert. Verify before you trust.</span>
      </div>
    </footer>
  );
}

