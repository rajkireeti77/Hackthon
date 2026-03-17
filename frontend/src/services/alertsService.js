import { apiFetch } from "./apiClient";

export async function ingestBackgroundContent(payload) {
  return await apiFetch("/api/background/ingest", {
    method: "POST",
    body: payload || {}
  });
}

export async function getUnreadAlerts() {
  return await apiFetch("/api/alerts/unread");
}

export async function getRecentAlerts(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, value);
    }
  });
  const query = params.toString();
  return await apiFetch(`/api/alerts${query ? `?${query}` : ""}`);
}

export async function getAlertDetails(id) {
  return await apiFetch(`/api/alerts/${id}`);
}

export async function markAlertRead(id) {
  return await apiFetch(`/api/alerts/${id}/read`, { method: "POST" });
}

export async function openAlertAnyway(id) {
  return await apiFetch(`/api/alerts/${id}/open-anyway`, { method: "POST" });
}

export async function cancelAlert(id) {
  return await apiFetch(`/api/alerts/${id}/cancel`, { method: "POST" });
}

export async function blockSender(id) {
  return await apiFetch(`/api/alerts/${id}/block-sender`, { method: "POST" });
}

export async function reportPhishing(id, reportNote = "") {
  return await apiFetch(`/api/alerts/${id}/report-phishing`, {
    method: "POST",
    body: { reportNote }
  });
}

export async function markAllAlertsRead() {
  return await apiFetch("/api/alerts/read-all", { method: "POST" });
}
