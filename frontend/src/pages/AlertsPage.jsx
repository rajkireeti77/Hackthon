import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import EmptyState from "../components/EmptyState";
import LoadingSpinner from "../components/LoadingSpinner";
import { getRecentAlerts, markAllAlertsRead } from "../services/alertsService";
import { formatDate, formatVerdict, scoreColor } from "../utils/format";

function verdictBadge(v) {
  if (v === "Safe") return "badge badge-safe";
  if (v === "LowRisk") return "badge badge-low";
  if (v === "Suspicious") return "badge badge-warn";
  return "badge badge-bad";
}

export default function AlertsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [filters, setFilters] = useState({
    sourceType: "",
    verdict: "",
    read: ""
  });

  async function load(nextFilters = filters) {
    setLoading(true);
    setError("");
    try {
      const data = await getRecentAlerts(nextFilters);
      setItems(Array.isArray(data) ? data : []);
    } catch (e) {
      setItems([]);
      setError(e?.message || "Failed to load alerts.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function onApplyFilters(event) {
    event.preventDefault();
    await load(filters);
  }

  async function onMarkAllRead() {
    if (busy) return;
    setBusy(true);
    try {
      await markAllAlertsRead();
      await load(filters);
    } catch (e) {
      setError(e?.message || "Could not mark alerts as read.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h2>Alert History</h2>
          <div className="muted">Incoming mail and message scans with verdicts, scores, and actions</div>
        </div>
        <button className="btn btn-ghost" onClick={onMarkAllRead} disabled={busy}>
          {busy ? "Updating..." : "Mark all read"}
        </button>
      </div>

      <form className="card filter-bar" onSubmit={onApplyFilters}>
        <label className="field">
          <span className="field-label">Source</span>
          <select
            className="input"
            value={filters.sourceType}
            onChange={(e) => setFilters((current) => ({ ...current, sourceType: e.target.value }))}
          >
            <option value="">All</option>
            <option value="EMAIL">Email</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="SMS">SMS</option>
            <option value="TELEGRAM">Telegram</option>
          </select>
        </label>

        <label className="field">
          <span className="field-label">Verdict</span>
          <select
            className="input"
            value={filters.verdict}
            onChange={(e) => setFilters((current) => ({ ...current, verdict: e.target.value }))}
          >
            <option value="">All</option>
            <option value="Safe">Safe</option>
            <option value="LowRisk">Low Risk</option>
            <option value="Suspicious">Suspicious</option>
            <option value="Malicious">Malicious</option>
          </select>
        </label>

        <label className="field">
          <span className="field-label">Read status</span>
          <select
            className="input"
            value={filters.read}
            onChange={(e) => setFilters((current) => ({ ...current, read: e.target.value }))}
          >
            <option value="">All</option>
            <option value="true">Read</option>
            <option value="false">Unread</option>
          </select>
        </label>

        <div className="actions filter-actions">
          <button className="btn btn-primary" type="submit">
            Apply
          </button>
          <button
            className="btn btn-ghost"
            type="button"
            onClick={() => {
              const next = { sourceType: "", verdict: "", read: "" };
              setFilters(next);
              load(next);
            }}
          >
            Reset
          </button>
        </div>
      </form>

      {error ? <div className="alert alert-bad">{error}</div> : null}

      {loading ? (
        <LoadingSpinner label="Loading alerts..." />
      ) : items.length ? (
        <section className="stack">
          {items.map((item) => (
            <article key={item.id} className="card alert-history-card">
              <div className="alert-history-top">
                <div>
                  <div className="card-title">
                    {item.popupTitle}
                    {!item.read ? <span className="pill pill-medium alert-new">Unread</span> : null}
                  </div>
                  <div className="muted small">
                    {item.sourceType} {item.senderLabel ? `• ${item.senderLabel}` : ""} • {formatDate(item.createdAt)}
                  </div>
                </div>
                <span className={verdictBadge(item.verdict)}>{formatVerdict(item.verdict)}</span>
              </div>

              <p className="summary">{item.shortExplanation || item.popupMessage}</p>

              <div className="grid-2">
                <div className="stat">
                  <div className="stat-k">Risk score</div>
                  <div className="stat-v" style={{ color: scoreColor(item.riskScore) }}>
                    {item.riskScore}
                  </div>
                </div>
                <div className="stat">
                  <div className="stat-k">Confidence</div>
                  <div className="stat-v">{Math.round((Number(item.confidenceScore) || 0) * 100)}%</div>
                </div>
                <div className="stat">
                  <div className="stat-k">URL status</div>
                  <div className="stat-v">{item.urlStatusSummary || "No URLs found"}</div>
                </div>
                <div className="stat">
                  <div className="stat-k">Safe to open</div>
                  <div className="stat-v">{item.safeToOpen ? "Yes" : "No"}</div>
                </div>
              </div>

              <div className="btn-row">
                <Link className="btn btn-primary" to={`/alerts/${item.id}`}>
                  View details
                </Link>
                <Link className="btn btn-ghost" to={`/scans/${item.analysisId}`}>
                  Open scan
                </Link>
              </div>
            </article>
          ))}
        </section>
      ) : (
        <EmptyState title="No alerts yet" description="Incoming message and email alerts will appear here after the background scanner processes them." />
      )}
    </div>
  );
}
