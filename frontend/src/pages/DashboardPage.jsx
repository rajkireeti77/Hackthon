import React, { useEffect, useMemo, useState } from "react";
import AnalysisForm from "../components/AnalysisForm";
import LoadingSpinner from "../components/LoadingSpinner";
import EmptyState from "../components/EmptyState";
import ResultSummaryCard from "../components/ResultSummaryCard";
import RedFlagCard from "../components/RedFlagCard";
import RecentScansTable from "../components/RecentScansTable";
import SecurityTipsPanel from "../components/SecurityTipsPanel";
import SafetyActions from "../components/SafetyActions";
import { analyzeContent, getRecentAnalyses } from "../services/analysisService";

export default function DashboardPage() {
  const [result, setResult] = useState(null);
  const [recent, setRecent] = useState([]);
  const [loadingAnalyze, setLoadingAnalyze] = useState(false);
  const [loadingRecent, setLoadingRecent] = useState(true);
  const [error, setError] = useState("");

  async function refreshRecent() {
    setLoadingRecent(true);
    try {
      const items = await getRecentAnalyses();
      setRecent(Array.isArray(items) ? items : []);
    } catch (e) {
      setRecent([]);
    } finally {
      setLoadingRecent(false);
    }
  }

  useEffect(() => {
    refreshRecent();
  }, []);

  async function onAnalyze(payload) {
    setError("");
    setLoadingAnalyze(true);
    try {
      const res = await analyzeContent(payload);
      setResult(res);
      await refreshRecent();
    } catch (e) {
      setResult(null);
      setError(e?.message || "Analysis failed.");
    } finally {
      setLoadingAnalyze(false);
    }
  }

  const redFlags = useMemo(() => {
    if (!result?.redFlags) return [];
    return Array.isArray(result.redFlags) ? result.redFlags : [];
  }, [result]);

  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h2>Dashboard</h2>
          <div className="muted">Scan emails, messages, URLs, and files for phishing or malware signals</div>
        </div>
      </div>

      <div className="grid-2 wide-left">
        <div className="stack">
          <AnalysisForm onAnalyze={onAnalyze} loading={loadingAnalyze} />

          {error ? <div className="alert alert-bad">{error}</div> : null}

          {result ? (
            <>
              <ResultSummaryCard result={result} />

              <section className="card">
                <div className="card-header">
                  <div>
                    <div className="card-title">Red flags</div>
                    <div className="muted">Signals detected in text, URLs, and attachments</div>
                  </div>
                </div>
                {redFlags.length ? (
                  <div className="flags-grid">
                    {redFlags.map((f, idx) => (
                      <RedFlagCard key={`${f.type}-${idx}`} flag={f} />
                    ))}
                  </div>
                ) : (
                  <EmptyState title="No red flags returned" description="Try scanning a message or URL to see results." />
                )}
              </section>

              <SafetyActions verdict={result.verdict} recommendedActions={result.recommendedActions} />
            </>
          ) : (
            <EmptyState
              title="No scan yet"
              description="Run your first scan to get a verdict, risk score, red flags, and safety advice."
            />
          )}
        </div>

        <div className="stack">
          {loadingRecent ? (
            <LoadingSpinner label="Loading recent scans..." />
          ) : recent.length ? (
            <RecentScansTable items={recent} />
          ) : (
            <EmptyState title="No scan history" description="Your recent scans will show up here after you analyze content." />
          )}

          <SecurityTipsPanel />
        </div>
      </div>
    </div>
  );
}

