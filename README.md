## PhishGuard AI

AI-powered phishing and social engineering detection for **email text**, **chat/text messages**, **URLs**, and **uploaded files/attachments**.

### Tech stack
- **Frontend**: React (Vite)
- **Backend**: Spring Boot
- **Database**: MySQL
- **AI**: Google Gemini 1.5 Flash API
- **URL reputation**: Google Safe Browsing API
- **Auth**: Firebase Auth (frontend), Firebase Admin (backend verification)

---

## Project structure
- `frontend/`: React app (Firebase Hosting ready)
- `backend/`: Spring Boot API (deploy separately)
- `database/`: SQL schema and notes
- `docs/`: Setup guides + prompt design
- `backend/ml/`: dataset downloaders and model training scripts for text, URL, and file-risk models

---

## Quick start (local)

### 1) MySQL
Create a database:

```sql
CREATE DATABASE phishguard_ai;
```

Apply schema (optional if you keep `JPA_DDL_AUTO=update`):
- See `database/schema.sql`

### 2) Backend
1) Copy env example:
- Copy `backend/.env.example` → `backend/.env` (don’t commit it)

2) Set required values:
- **MySQL**: `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`
- **Firebase Admin**: `FIREBASE_SERVICE_ACCOUNT_PATH`
- **Gemini**: `GEMINI_API_KEY`
- **Safe Browsing**: `SAFE_BROWSING_API_KEY`

3) Run Spring Boot (Java 17 + Maven required):

```bash
cd backend
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`.

The backend now loads trained lightweight models by default from:
- `backend/text-model-weights.json`
- `backend/url-model-weights.json`
- `backend/file-model-weights.json`

### 3) Frontend
1) Copy env example:
- Copy `frontend/.env.example` → `frontend/.env`

2) Fill Firebase web config + backend URL:
- `VITE_API_BASE_URL=http://localhost:8080`

3) Run:

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`.

---

## API docs and setup guides
- **API usage + auth**: `docs/api-setup.md`
- **Firebase setup**: `docs/firebase-setup.md`
- **Gemini prompt design**: `docs/gemini-prompt-design.md`
- **MySQL schema**: `database/schema.sql`

## Retraining models
From `backend/ml/`:

```bash
python download_datasets.py --out-dir data
python train_text_model.py --csv data/sms.csv --out ../text-model-weights.json
python train_url_model.py --csv data/urls.csv --out ../url-model-weights.json
python train_file_model.py --csv data/files.csv --out ../file-model-weights.json
```

## Local LLM Fine-Tuning
This project can also call a locally fine-tuned LLM for the final explainable analysis step.

From `backend/ml/`:

```bash
pip install -r requirements-llm.txt
python prepare_llm_dataset.py --out-dir llm-data
python train_llm_model.py --train-jsonl llm-data/train.jsonl --valid-jsonl llm-data/valid.jsonl --out-dir llm-artifacts/phishguard-lora
python llm_inference_server.py --adapter-dir llm-artifacts/phishguard-lora
```

Then enable it for the Spring Boot backend:

```bash
LOCAL_LLM_ENABLED=true
LOCAL_LLM_BASE_URL=http://localhost:8001
```

Behavior:
- If `LOCAL_LLM_ENABLED=true`, the backend calls the local LLM first.
- If the local LLM is unavailable, the backend falls back to Gemini.
- If Gemini is not configured, it falls back to the built-in heuristic analyzer.

## Sandbox URL Scanning
For suspicious URLs, you can run a separate isolated scanner that opens links in a disposable headless browser and reports behavior such as redirects, login forms, downloads, and suspicious keywords.

Start the scanner:

```bash
cd sandbox-scanner
npm install
npm start
```

Then enable it for the backend:

```bash
SANDBOX_SCANNER_ENABLED=true
SANDBOX_SCANNER_BASE_URL=http://localhost:8002
```

## Background Alerts
The app can now accept background-ingested content, automatically poll an email inbox, watch Windows desktop notifications, and show popup warnings in the frontend.

Main endpoint:

```bash
POST /api/background/ingest
```

Use it for:
- email webhook or IMAP connector
- Android notification-listener companion app for WhatsApp / SMS
- download watcher for suspicious APK or attachments

Alert actions:
- `GET /api/alerts/{id}`
- `POST /api/alerts/{id}/open-anyway`
- `POST /api/alerts/{id}/cancel`
- `POST /api/alerts/{id}/block-sender`
- `POST /api/alerts/{id}/report-phishing`

Automatic email polling:

```bash
INCOMING_MAIL_ENABLED=true
INCOMING_MAIL_USER_ID=demo
INCOMING_MAIL_HOST=imap.gmail.com
INCOMING_MAIL_PORT=993
INCOMING_MAIL_PROTOCOL=imaps
INCOMING_MAIL_USERNAME=your_mailbox@example.com
INCOMING_MAIL_PASSWORD=your_app_password
```

When enabled, Spring Boot polls the configured mailbox, reads unseen emails, extracts URLs and attachment metadata, analyzes them, and creates the same alert history entries that manual background ingestion creates.

Automatic Windows notification polling:

```bash
WINDOWS_NOTIFICATIONS_ENABLED=true
WINDOWS_NOTIFICATIONS_USER_ID=demo
WINDOWS_NOTIFICATIONS_POLL_INTERVAL_MS=5000
WINDOWS_NOTIFICATIONS_ALLOWED_APPS=5319275A.WhatsAppDesktop_cv1g1gvanyjgm!App
```

When enabled on Windows, the same Spring Boot backend polls the local Windows notification database and automatically ingests new WhatsApp Desktop toast notifications into the existing alert flow.

Alert endpoints:
- `GET /api/alerts`
- `GET /api/alerts/unread`
- `POST /api/alerts/{id}/read`
- `POST /api/alerts/read-all`

More details:
- `docs/background-monitoring.md`

