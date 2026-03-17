import React, { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import ResultSummaryCard from "../components/ResultSummaryCard";
import RedFlagCard from "../components/RedFlagCard";
import SafetyActions from "../components/SafetyActions";
import { deleteAnalysisById, getAnalysisById } from "../services/analysisService";
import { formatDate } from "../utils/format";

export default function ScanDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const res = await getAnalysisById(id);
        if (active) setItem(res);
      } catch (e) {
        if (active) setError(e?.message || "Failed to load scan.");
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, [id]);

  async function onDelete() {
    if (deleting) return;
    const ok = window.confirm("Delete this scan record?");
    if (!ok) return;
    setDeleting(true);
    try {
      await deleteAnalysisById(id);
      navigate("/dashboard");
    } catch (e) {
      setError(e?.message || "Delete failed.");
    } finally {
      setDeleting(false);
    }
  }

  if (loading) return <LoadingSpinner label="Loading scan details..." />;

  if (error) {
    return (
      <div className="stack">
        <div className="alert alert-bad">{error}</div>
        <Link className="btn btn-ghost" to="/dashboard">
          Back to dashboard
        </Link>
      </div>
    );
  }

  if (!item) return null;

  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h2>Scan Details</h2>
          <div className="muted">
            Scan <span className="mono">#{item.id}</span> • {formatDate(item.createdAt)}
          </div>
        </div>
        <div className="btn-row">
          <Link className="btn btn-ghost" to="/dashboard">
            Back
          </Link>
          <button className="btn btn-bad" onClick={onDelete} disabled={deleting}>
            {deleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>

      <div className="grid-2 wide-left">
        <div className="stack">
          <ResultSummaryCard result={item} />

          <section className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Red flags</div>
                <div className="muted">Why this content was scored this way</div>
              </div>
            </div>
            <div className="flags-grid">
              {(item.redFlags || []).map((f, idx) => (
                <RedFlagCard key={`${f.type}-${idx}`} flag={f} />
              ))}
            </div>
          </section>
        </div>

        <div className="stack">
          <section className="card">
            <div className="card-header">
              <div>
                <div className="card-title">URL reputation</div>
                <div className="muted">Google Safe Browsing result</div>
              </div>
            </div>
            <div className="grid-2">
              <div className="stat">
                <div className="stat-k">Flagged</div>
                <div className="stat-v">{item.safeBrowsingResult?.flagged ? "Yes" : "No"}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Threat types</div>
                <div className="stat-v mono">
                  {(item.safeBrowsingResult?.threatTypes || []).length
                    ? (item.safeBrowsingResult.threatTypes || []).join(", ")
                    : "None"}
                </div>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="card-header">
              <div>
                <div className="card-title">Sandbox URL scan</div>
                <div className="muted">Behavior seen in an isolated browser environment</div>
              </div>
            </div>
            <div className="grid-2">
              <div className="stat">
                <div className="stat-k">Verdict</div>
                <div className="stat-v">{item.sandboxScanResult?.verdict || "UNAVAILABLE"}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Risk score</div>
                <div className="stat-v">{item.sandboxScanResult?.riskScore ?? 0}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Final URL</div>
                <div className="stat-v mono">{item.sandboxScanResult?.finalUrl || "Not scanned"}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Redirects</div>
                <div className="stat-v">{item.sandboxScanResult?.redirectCount ?? 0}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Login form</div>
                <div className="stat-v">{item.sandboxScanResult?.loginFormDetected ? "Yes" : "No"}</div>
              </div>
              <div className="stat">
                <div className="stat-k">Download attempt</div>
                <div className="stat-v">{item.sandboxScanResult?.downloadAttempted ? "Yes" : "No"}</div>
              </div>
            </div>
            <p className="summary">{item.sandboxScanResult?.summary || "Sandbox scanner was not used for this scan."}</p>
          </section>

          <SafetyActions verdict={item.verdict} recommendedActions={item.recommendedActions} />
        </div>
      </div>
    </div>
  );
}

