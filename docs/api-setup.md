## API setup guide (Spring Boot + Firebase Auth)

### Base URL
Default local backend:
- `http://localhost:8080`

Frontend uses:
- `VITE_API_BASE_URL` in `frontend/.env`

---

## Authentication model
The frontend authenticates users using **Firebase Auth (email/password)**.

The backend requires a Firebase **ID token** on every API request:
- Header: `Authorization: Bearer <firebase_id_token>`

Backend validates this token using **Firebase Admin SDK** and uses the decoded `uid` as `userId` for scan records.

---

## Endpoints

### `POST /api/analyze`
Analyze text, url, or both.

Request body:
```json
{
  "textContent": "Your bank account will be blocked. Click immediately.",
  "url": "https://example.com"
}
```

Response body (example):
```json
{
  "id": 1,
  "verdict": "Malicious",
  "riskScore": 92,
  "severity": "Critical",
  "confidence": 0.95,
  "summary": "This message uses urgency and impersonation tactics to trick the user into revealing sensitive information.",
  "redFlags": [
    { "type": "Urgency Detected", "severity": "High", "description": "The message pressures the user to act immediately." }
  ],
  "safeBrowsingResult": { "flagged": true, "threatTypes": ["SOCIAL_ENGINEERING"] },
  "recommendedActions": ["Do not click the link", "Verify the sender through official channels"],
  "createdAt": "2026-03-17T10:00:00Z"
}
```

### `GET /api/analyses`
Returns up to the latest 20 scans for the current user.

### `GET /api/analyses/{id}`
Fetch one scan (only if it belongs to the current user).

### `DELETE /api/analyses/{id}`
Delete one scan (only if it belongs to the current user).

---

## External API keys setup (required)

### Gemini API key
1) Create/obtain a Gemini API key in Google AI Studio / Google Cloud (Generative Language API enabled)
2) Set in `backend/.env`:
- `GEMINI_API_KEY=...`

If missing, backend will return `502` with message like “Gemini API key is not configured.”

### Safe Browsing API key
1) Enable Google Safe Browsing API in Google Cloud project
2) Create an API key
3) Set in `backend/.env`:
- `SAFE_BROWSING_API_KEY=...`

If missing and you submit a URL, backend will return `502` with message like “Safe Browsing API key is not configured.”

---

## Getting a Firebase ID token (for Postman/curl testing)

### Option A (recommended): Use the frontend
1) Run the frontend.
2) Login.
3) In browser devtools console, run:

```js
await firebase.auth().currentUser.getIdToken()
```

If you’re using Firebase v9 modules (this project does), a quick way is:

```js
import { getAuth } from "firebase/auth";
await getAuth().currentUser.getIdToken();
```

Copy the token and use it in Postman as:
- `Authorization: Bearer <token>`

### Option B: Use Firebase Auth REST API
You can exchange email/password for an ID token using Google’s Identity Toolkit REST API.
Search for “Identity Toolkit signInWithPassword REST” in Firebase docs.

---

## curl examples

Replace `$TOKEN` with your Firebase ID token:

### Analyze
```bash
curl -X POST "http://localhost:8080/api/analyze" ^
  -H "Authorization: Bearer $TOKEN" ^
  -H "Content-Type: application/json" ^
  -d "{\"textContent\":\"Your account will be suspended. Verify now.\",\"url\":\"https://example.com\"}"
```

### List
```bash
curl "http://localhost:8080/api/analyses" -H "Authorization: Bearer $TOKEN"
```

### Get by id
```bash
curl "http://localhost:8080/api/analyses/1" -H "Authorization: Bearer $TOKEN"
```

### Delete
```bash
curl -X DELETE "http://localhost:8080/api/analyses/1" -H "Authorization: Bearer $TOKEN"
```

