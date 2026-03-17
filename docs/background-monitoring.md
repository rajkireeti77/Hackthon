# Background Monitoring

This project can analyze content in the background, classify every extracted URL, save the result, and create popup alerts inside the web app.

## What works now

- External systems can push new content into:
  - `POST /api/background/ingest`
- The backend can also poll an email inbox directly via IMAP when `INCOMING_MAIL_ENABLED=true`
- The backend can also watch the local Windows notification database when `WINDOWS_NOTIFICATIONS_ENABLED=true`
- The backend automatically:
  - reads the full message
  - extracts all URLs from text / HTML
  - validates each URL as valid, redirected, shortened, spoofed, suspicious, or malicious
  - analyzes the full message for phishing and social engineering
  - stores the normalized event, URL evidence, scan result, and alert history
  - creates an unread security alert
- The frontend polls unread alerts and shows a small popup with:
  - verdict
  - risk score
  - confidence score
  - short explanation
  - URL status summary
  - safe-to-open guidance
  - action buttons

## Example ingest payload

```json
{
  "sourceType": "WHATSAPP",
  "sourceMessageId": "wa-12345",
  "senderLabel": "+91 9876543210",
  "senderAddress": "+91 9876543210",
  "subjectLine": "Delivery failed",
  "textContent": "Your package is on hold. Open the attached APK to track it.",
  "htmlContent": "<p>Your package is on hold. <a href=\"http://secure-track-package.top/login\">Track now</a></p>",
  "url": "http://secure-track-package.top/login",
  "attachments": [
    {
      "fileName": "tracking_update.apk",
      "contentType": "application/vnd.android.package-archive",
      "sizeBytes": 204800
    }
  ]
}
```

## Example local test

```bash
curl -X POST http://localhost:8080/api/background/ingest \
  -H "Content-Type: application/json" \
  -d "{\"sourceType\":\"EMAIL\",\"sourceMessageId\":\"mail-8844\",\"senderLabel\":\"support@fakebank.com\",\"senderAddress\":\"support@fakebank.com\",\"subjectLine\":\"Urgent verification\",\"textContent\":\"Verify your account now via https://bit.ly/fakebank-reset\",\"htmlContent\":\"<p>Verify your account now via <a href='https://bit.ly/fakebank-reset'>this link</a></p>\"}"
```

## Automatic email polling

Enable these backend env vars:

```bash
INCOMING_MAIL_ENABLED=true
INCOMING_MAIL_USER_ID=demo
INCOMING_MAIL_HOST=imap.gmail.com
INCOMING_MAIL_PORT=993
INCOMING_MAIL_PROTOCOL=imaps
INCOMING_MAIL_USERNAME=your_mailbox@example.com
INCOMING_MAIL_PASSWORD=your_app_password
INCOMING_MAIL_FOLDER=INBOX
INCOMING_MAIL_POLL_INTERVAL_MS=30000
INCOMING_MAIL_MAX_MESSAGES=10
INCOMING_MAIL_MARK_SEEN=true
```

When enabled, the Spring Boot backend polls the configured inbox and feeds each unseen email into the same analysis and alert pipeline used by background ingestion.

## Automatic Windows notification polling

Enable these backend env vars:

```bash
WINDOWS_NOTIFICATIONS_ENABLED=true
WINDOWS_NOTIFICATIONS_USER_ID=demo
WINDOWS_NOTIFICATIONS_POLL_INTERVAL_MS=5000
WINDOWS_NOTIFICATIONS_ALLOWED_APPS=5319275A.WhatsAppDesktop_cv1g1gvanyjgm!App
WINDOWS_NOTIFICATIONS_IGNORED_PAYLOAD_TYPES=badge,tile
```

By default the backend reads:

```text
%LOCALAPPDATA%\\Microsoft\\Windows\\Notifications\\wpndatabase.db
```

This lets the same Spring Boot backend ingest new WhatsApp Desktop toast notifications directly on the same Windows machine and route them through the existing alert pipeline.

## What is still needed for real WhatsApp / SMS auto-monitoring

The current project is a web app, so it cannot directly read:
- WhatsApp notifications from your phone
- incoming SMS
- downloaded APK files from another app

To do that in production, add a connector:

### Android companion app
- Uses Notification Listener access
- Detects WhatsApp / SMS / APK notifications
- Extracts sender, text, links, and attachment metadata
- Sends them to `/api/background/ingest`

### Desktop / browser connector
- Watches downloaded files or extension events
- Sends metadata to `/api/background/ingest`

## Recommended production flow

1. Either the built-in IMAP poller or a connector receives a new email / WhatsApp / SMS / attachment event.
2. It forwards the content to `/api/background/ingest`.
3. PhishGuard AI analyzes it.
4. The frontend shows a popup warning with action buttons.
5. The user opens alert details or the saved scan record if needed.
