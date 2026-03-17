import React, { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import ResultSummaryCard from "../components/ResultSummaryCard";
import SafetyActions from "../components/SafetyActions";
import {
  blockSender,
  cancelAlert,
  getAlertDetails,
  openAlertAnyway,
  reportPhishing
} from "../services/alertsService";
import { formatDate, formatVerdict, scoreColor } from "../utils/format";

function verdictBadge(v) {
  if (v === "Safe") return "badge badge-safe";
  if (v === "LowRisk") return "badge badge-low";
  if (v === "Suspicious") return "badge badge-warn";
  return "badge badge-bad";
}

export default function AlertDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try {
      const result = await getAlertDetails(id);
      setItem(result);
    } catch (e) {
      setError(e?.message || "Failed to load alert details.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [id]);

  async function handleAction(name, fn) {
    if (busyAction) return;
    setBusyAction(name);
    setError("");
    try {
      await fn();
      await load();
    } catch (e) {
      setError(e?.message || "Action failed.");
    } finally {
      setBusyAction("");
    }
  }

  async function onOpenAnyway() {
    await handleAction("open", async () => {
      await openAlertAnyway(id);
      if (item?.primaryTargetUrl) {
        window.open(item.primaryTargetUrl, "_blank", "noopener,noreferrer");
      } else {
        navigate(`/scans/${item.analysisId}`);
      }
    });
  }

  if (loading) return <LoadingSpinner label="Loading alert details..." />;

  if (error && !item) {
    return (
      <div className="stack">
        <div className="alert alert-bad">{error}</div>
        <Link className="btn btn-ghost" to="/alerts">
          Back to alerts
        </Link>
      </div>
    );
  }

  if (!item) return null;

  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h2>Alert Details</h2>
          <div className="muted">
            {item.sourceType} {item.senderLabel ? `• ${item.senderLabel}` : ""} • {formatDate(item.createdAt)}
          </div>
        </div>
        <div className="btn-row">
          <Link className="btn btn-ghost" to="/alerts">
            Back
          </Link>
          <Link className="btn btn-ghost" to={`/scans/${item.analysisId}`}>
            Open scan
          </Link>
        </div>
      </div>

      {error ? <div className="alert alert-bad">{error}</div> : null}

      <section className="card">
        <div className="alert-history-top">
          <div>
            <div className="card-title">{item.popupTitle}</div>
            <div className="muted small">
              {item.subjectLine || "No subject"} • {item.sourceMessageId || "No source message id"}
            </div>
          </div>
          <span className={verdictBadge(item.verdict)}>{formatVerdict(item.verdict)}</span>
        </div>

        <p className="summary">{item.popupMessage}</p>

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
            <div className="stat-k">Safe to open</div>
            <div className="stat-v">{item.safeToOpen ? "Yes" : "No"}</div>
          </div>
          <div className="stat">
            <div className="stat-k">URL status</div>
            <div className="stat-v">{item.urlStatusSummary || "No URLs found"}</div>
          </div>
        </div>

        <div className="divider" />
        <div className="btn-row">
          <button className="btn btn-primary" onClick={onOpenAnyway} disabled={busyAction === "open"}>
            {busyAction === "open" ? "Opening..." : "Open Anyway"}
          </button>
          <button
            className="btn btn-ghost"
            onClick={() => handleAction("cancel", () => cancelAlert(id))}
            disabled={busyAction === "cancel"}
          >
            {busyAction === "cancel" ? "Cancelling..." : "Cancel"}
          </button>
          <button
            className="btn btn-warn"
            onClick={() => navigate(`/alerts/${id}`)}
            disabled
            title="Already viewing details"
          >
            View Details
          </button>
          <button
            className="btn btn-bad"
            onClick={() => handleAction("block", () => blockSender(id))}
            disabled={busyAction === "block" || item.blockedSender}
          >
            {item.blockedSender ? "Sender Blocked" : busyAction === "block" ? "Blocking..." : "Block Sender"}
          </button>
          <button
            className="btn btn-bad"
            onClick={() => handleAction("report", () => reportPhishing(id, "Reported from alert details view"))}
            disabled={busyAction === "report" || item.reportedPhishing}
          >
            {item.reportedPhishing ? "Reported" : busyAction === "report" ? "Reporting..." : "Report Phishing"}
          </button>
        </div>
      </section>

      <div className="grid-2 wide-left">
        <div className="stack">
          <section className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Incoming message</div>
                <div className="muted">Normalized sender and message details</div>
              </div>
            </div>
            <div className="grid-2">
              <div className="stat">
                <div className="stat-k">Sender label</div>
                <div className="stat-v">{item.senderLabel || "Unknown"}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Sender address</div>
                <div className="stat-v mono">{item.senderAddress || "Unavailable"}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Received at</div>
                <div className="stat-v mono">{formatDate(item.receivedAt)}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Attachment</div>
                <div className="stat-v">{item.fileName || "None"}</div>
              </div>
            </div>
            <div className="divider" />
            <div className="card-title">Preview</div>
            <p className="summary">{item.previewText || "No preview available."}</p>
          </section>

          <section className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Extracted URLs</div>
                <div className="muted">Each detected link with validation and reputation evidence</div>
              </div>
            </div>
            {item.urls?.length ? (
              <div className="url-list">
                {item.urls.map((url, idx) => (
                  <article key={`${url.originalUrl}-${idx}`} className="url-card">
                    <div className="url-card-top">
                      <span className={`pill ${url.statusLabel === "MALICIOUS" ? "pill-critical" : url.statusLabel === "SUSPICIOUS" || url.statusLabel === "SPOOFED" ? "pill-high" : "pill-low"}`}>
                        {url.statusLabel}
                      </span>
                      <span className="mono" style={{ color: scoreColor(url.riskScore) }}>
                        {url.riskScore}/100
                      </span>
                    </div>
                    <div className="mono url-line">{url.originalUrl}</div>
                    {url.finalUrl && url.finalUrl !== url.originalUrl ? (
                      <div className="muted small">Final destination: {url.finalUrl}</div>
                    ) : null}
                    <div className="url-chip-row">
                      <span className="pill pill-low">{url.valid ? "Valid" : "Invalid"}</span>
                      {url.shortened ? <span className="pill pill-medium">Shortened</span> : null}
                      {url.redirected ? <span className="pill pill-medium">Redirected</span> : null}
                      {url.spoofed ? <span className="pill pill-high">Spoofed</span> : null}
                      {url.safeBrowsingFlagged ? <span className="pill pill-critical">Safe Browsing Flag</span> : null}
                    </div>
                    {url.evidence?.length ? (
                      <ul className="bullets compact-bullets">
                        {url.evidence.map((line, lineIdx) => (
                          <li key={`${line}-${lineIdx}`}>{line}</li>
                        ))}
                      </ul>
                    ) : null}
                  </article>
                ))}
              </div>
            ) : (
              <div className="muted">No URLs were extracted from this message.</div>
            )}
          </section>
        </div>

        <div className="stack">
          <ResultSummaryCard result={item.analysis} />
          <SafetyActions verdict={item.analysis?.verdict} recommendedActions={item.analysis?.recommendedActions} />
        </div>
      </div>
    </div>
  );
}
