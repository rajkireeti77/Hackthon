## Gemini prompt design (Gemini 1.5 Flash)

### Goals
We want Gemini to:
- Detect phishing/social engineering signals in text + URL context
- Return **structured JSON only** (no markdown)
- Provide **explainable** and **actionable** output:
  - verdict: Safe | Suspicious | Malicious
  - riskScore: 0–100
  - severity: Low | Medium | High | Critical
  - confidence: 0.0–1.0
  - summary: simple English, max 2 sentences
  - redFlags[]: {type, severity, description}
  - recommendedActions[]: 3–6 short actions

### Why structured JSON
Structured output makes it:
- easy to parse into typed DTOs
- consistent for UI rendering
- testable in Postman/curl

---

## Final prompt used by the backend
The backend builds a single user prompt and requests:
- low temperature (0.2)
- `responseMimeType: application/json`

This is the exact prompt template used in `backend/src/main/java/com/phishguardai/backend/service/GeminiClient.java`:

```text
You are PhishGuard AI, a cybersecurity assistant specialized in phishing and social engineering detection.

Analyze the following user-provided content and produce ONLY a strict JSON object (no markdown, no code fences, no extra text).

Inputs:
- textContent: <JSON-quoted string or null>
- url: <JSON-quoted string or null>
- safeBrowsingFlagged: <true/false>
- safeBrowsingThreatTypes: <list or []>

Task:
1) Detect phishing/social engineering tactics: urgency, fear, authority impersonation, credential theft intent, suspicious tone, scam indicators, spoofing.
2) Decide a final verdict: Safe | Suspicious | Malicious
3) Provide an explainable summary in simple English (max 2 sentences).
4) Provide redFlags: array of objects { type, severity, description } where severity is Low|Medium|High|Critical.
5) Provide recommendedActions: 3-6 short bullet-like strings.
6) Provide riskScore: integer 0-100.
7) Provide confidence: number 0.0-1.0.
8) Provide severity: Low | Medium | High | Critical.

Required JSON schema:
{
  "verdict": "Safe|Suspicious|Malicious",
  "riskScore": 0,
  "severity": "Low|Medium|High|Critical",
  "confidence": 0.0,
  "summary": "string",
  "redFlags": [{"type":"string","severity":"Low|Medium|High|Critical","description":"string"}],
  "recommendedActions": ["string"]
}
```

---

## Parsing strategy (important)
Gemini sometimes returns JSON inside code fences. The backend defensively:
- trims whitespace
- strips starting/ending ``` fences if present
- parses JSON and clamps values:
  - riskScore clamped to 0–100
  - confidence clamped to 0.0–1.0

---

## Practical guidance to keep output reliable
- Keep “ONLY JSON” instruction early and explicit.
- Include a strict schema and require exact enum values.
- Provide Safe Browsing context to help Gemini explain URL-based risk.
- Keep token budget reasonable; truncate very long text (backend truncates prompt input).

