export function formatDate(isoString) {
  if (!isoString) return "";
  const d = new Date(isoString);
  if (Number.isNaN(d.getTime())) return isoString;
  return d.toLocaleString();
}

export function scoreColor(score) {
  const s = Number(score) || 0;
  if (s <= 19) return "var(--good)";
  if (s <= 39) return "var(--warn)";
  if (s <= 69) return "var(--risk)";
  return "var(--bad)";
}

export function formatVerdict(verdict) {
  if (verdict === "LowRisk") return "Low Risk";
  return verdict || "Unknown";
}

