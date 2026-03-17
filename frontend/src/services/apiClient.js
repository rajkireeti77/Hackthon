const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function getToken() {
  const authDisabled =
    import.meta.env.VITE_DISABLE_AUTH === "true" || import.meta.env.DEV;
  if (authDisabled) return null;

  const { auth } = await import("../firebase.js");
  const u = auth.currentUser;
  if (!u) return null;
  return await u.getIdToken();
}

export async function apiFetch(path, { method = "GET", body, headers } = {}) {
  const token = await getToken();
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(headers || {})
    },
    body: body ? JSON.stringify(body) : undefined
  });

  const contentType = res.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const data = isJson ? await res.json().catch(() => null) : await res.text().catch(() => null);

  if (!res.ok) {
    const message =
      (data && typeof data === "object" && data.message) ||
      (typeof data === "string" && data) ||
      `Request failed (${res.status})`;
    const err = new Error(message);
    err.status = res.status;
    err.data = data;
    throw err;
  }

  return data;
}

