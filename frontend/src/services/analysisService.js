import { apiFetch } from "./apiClient";

export async function analyzeContent(payload) {
  return await apiFetch("/api/analyze", {
    method: "POST",
    body: payload || {}
  });
}

export async function getRecentAnalyses() {
  return await apiFetch("/api/analyses");
}

export async function getAnalysisById(id) {
  return await apiFetch(`/api/analyses/${id}`);
}

export async function deleteAnalysisById(id) {
  return await apiFetch(`/api/analyses/${id}`, { method: "DELETE" });
}

