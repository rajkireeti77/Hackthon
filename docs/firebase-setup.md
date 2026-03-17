## Firebase setup guide (Auth + Admin verification)

PhishGuard AI uses Firebase in two places:
- **Frontend**: Firebase Auth (email/password)
- **Backend**: Firebase Admin SDK to verify the `Authorization: Bearer <idToken>` header

---

## 1) Create a Firebase project
1) Go to Firebase Console
2) Create a project (or reuse an existing one)

---

## 2) Enable Email/Password Authentication
1) Firebase Console → **Build → Authentication**
2) **Sign-in method**
3) Enable **Email/Password**

---

## 3) Create Firebase Web App (for frontend config)
1) Firebase Console → Project settings
2) Under “Your apps”, add a **Web app**
3) Copy the config values into `frontend/.env`:
- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`
- `VITE_FIREBASE_APP_ID`

---

## 4) Create a Service Account key (for backend verification)
1) Firebase Console → Project settings → **Service accounts**
2) Click **Generate new private key**
3) Save the JSON file somewhere safe on your machine (do NOT commit it)
4) Set in `backend/.env`:
- `FIREBASE_SERVICE_ACCOUNT_PATH=C:\path\to\firebase-service-account.json`

Optional:
- `FIREBASE_PROJECT_ID=your-project-id`

---

## 5) (Optional) Local Hosting / Auth domains
For local dev, ensure your Firebase Auth settings allow the local domain.
Firebase generally supports localhost by default for Auth flows; email/password works without redirects.

---

## 6) Verify backend auth is working
When you call backend endpoints, always include:
- `Authorization: Bearer <Firebase ID token>`

If you get `401 Missing Bearer token`:
- You didn’t send `Authorization` header

If you get `401 Invalid or expired token`:
- Token is invalid/expired OR backend Firebase Admin is not configured properly

