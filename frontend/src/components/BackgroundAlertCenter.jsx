import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
  blockSender,
  cancelAlert,
  getUnreadAlerts,
  markAlertRead,
  openAlertAnyway,
  reportPhishing
} from "../services/alertsService";
import { formatVerdict } from "../utils/format";

const POLL_MS = 12000;

function severityClass(severity) {
  const value = String(severity || "").toLowerCase();
  if (value === "critical") return "pill pill-critical";
  if (value === "high") return "pill pill-high";
  if (value === "medium") return "pill pill-medium";
  return "pill pill-low";
}

function verdictBadge(v) {
  if (v === "Safe") return "badge badge-safe";
  if (v === "LowRisk") return "badge badge-low";
  if (v === "Suspicious") return "badge badge-warn";
  return "badge badge-bad";
}

export default function BackgroundAlertCenter() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [busyAction, setBusyAction] = useState("");
  const shownIds = useRef(new Set());
  const browserNotified = useRef(new Set());

  useEffect(() => {
    if (!user) return;

    let active = true;
    async function requestPermissionIfNeeded() {
      if (!("Notification" in window)) return;
      if (Notification.permission === "default") {
        try {
          await Notification.requestPermission();
        } catch {}
      }
    }

    async function poll() {
      try {
        const items = await getUnreadAlerts();
        if (!active) return;
        const next = Array.isArray(items) ? items : [];
        const fresh = next.filter((item) => !shownIds.current.has(item.id));
        if (fresh.length) {
          setAlerts((current) => [...fresh, ...current].slice(0, 5));
          fresh.forEach((item) => shownIds.current.add(item.id));
          if ("Notification" in window && Notification.permission === "granted") {
            fresh.forEach((item) => {
              if (browserNotified.current.has(item.id)) return;
              const notification = new Notification(item.popupTitle, {
                body: item.popupMessage
              });
              notification.onclick = () => {
                window.focus();
                navigate(`/alerts/${item.id}`);
              };
              browserNotified.current.add(item.id);
            });
          }
        }
      } catch {}
    }

    requestPermissionIfNeeded();
    poll();
    const timer = window.setInterval(poll, POLL_MS);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [navigate, user]);

  function removeAlert(alertId) {
    setAlerts((current) => current.filter((item) => item.id !== alertId));
  }

  async function runAction(actionKey, alert, callback) {
    const key = `${actionKey}:${alert.id}`;
    if (busyAction) return;
    setBusyAction(key);
    try {
      await callback();
      removeAlert(alert.id);
    } catch {
    } finally {
      setBusyAction("");
    }
  }

  async function onViewDetails(alert) {
    try {
      await markAlertRead(alert.id);
    } catch {}
    removeAlert(alert.id);
    navigate(`/alerts/${alert.id}`);
  }

  async function onOpenAnyway(alert) {
    await runAction("open", alert, async () => {
      await openAlertAnyway(alert.id);
      if (alert.primaryTargetUrl) {
        window.open(alert.primaryTargetUrl, "_blank", "noopener,noreferrer");
      } else {
        navigate(`/alerts/${alert.id}`);
      }
    });
  }

  if (!user || alerts.length === 0) return null;

  return (
    <div className="alert-center" aria-live="polite">
      {alerts.map((alert) => {
        const isBusy = busyAction.endsWith(`:${alert.id}`);
        return (
          <section key={alert.id} className="alert-toast">
            <div className="alert-toast-top">
              <div>
                <div className="alert-toast-title">{alert.popupTitle}</div>
                <div className="muted small">
                  {alert.sourceType || "BACKGROUND"} {alert.senderLabel ? `• ${alert.senderLabel}` : ""}
                </div>
              </div>
              <span className={severityClass(alert.severity)}>{alert.severity}</span>
            </div>

            <div className="alert-toast-badges">
              <span className={verdictBadge(alert.verdict)}>{formatVerdict(alert.verdict)}</span>
              <span className="pill pill-low">Risk {alert.riskScore}</span>
              <span className="pill pill-low">Confidence {Math.round((Number(alert.confidenceScore) || 0) * 100)}%</span>
            </div>

            <p className="alert-toast-body">{alert.shortExplanation || alert.popupMessage}</p>
            <div className="alert-toast-meta">
              <div>
                <span className="muted">URL status:</span> {alert.urlStatusSummary || "No URLs found"}
              </div>
              <div>
                <span className="muted">Safe to open:</span> {alert.safeToOpen ? "Yes" : "No"}
              </div>
            </div>

            {alert.fileName ? <div className="mono small">File: {alert.fileName}</div> : null}

            <div className="alert-toast-actions wrap">
              <button className="btn btn-small btn-primary" onClick={() => onOpenAnyway(alert)} disabled={isBusy}>
                Open Anyway
              </button>
              <button
                className="btn btn-small btn-ghost"
                onClick={() => runAction("cancel", alert, () => cancelAlert(alert.id))}
                disabled={isBusy}
              >
                Cancel
              </button>
              <button className="btn btn-small btn-ghost" onClick={() => onViewDetails(alert)} disabled={isBusy}>
                View Details
              </button>
              <button
                className="btn btn-small btn-warn"
                onClick={() => runAction("block", alert, () => blockSender(alert.id))}
                disabled={isBusy || alert.blockedSender}
              >
                {alert.blockedSender ? "Blocked" : "Block Sender"}
              </button>
              <button
                className="btn btn-small btn-bad"
                onClick={() => runAction("report", alert, () => reportPhishing(alert.id, "Reported from popup"))}
                disabled={isBusy || alert.reportedPhishing}
              >
                {alert.reportedPhishing ? "Reported" : "Report Phishing"}
              </button>
            </div>
          </section>
        );
      })}
    </div>
  );
}
