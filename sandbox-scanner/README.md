# Sandbox URL Scanner

This service opens submitted URLs in an isolated headless Chromium instance and reports behavioral signals back to the Spring Boot backend.

## Run locally

```bash
cd sandbox-scanner
npm install
npm start
```

Service URL:
- `http://localhost:8002`

Health check:
- `GET /health`

Analyze endpoint:
- `POST /analyze`

Example body:

```json
{
  "url": "https://example.com"
}
```

## Run with Docker

```bash
cd sandbox-scanner
docker build -t phishguard-sandbox-scanner .
docker run --rm -p 8002:8002 phishguard-sandbox-scanner
```

## Backend integration

Enable these env vars for the backend:

```bash
SANDBOX_SCANNER_ENABLED=true
SANDBOX_SCANNER_BASE_URL=http://localhost:8002
SANDBOX_SCANNER_ANALYZE_PATH=/analyze
```
