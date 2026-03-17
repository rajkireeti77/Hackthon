## PhishGuard AI Frontend (React)

### Setup

1) Install dependencies

```bash
cd frontend
npm install
```

2) Create `.env` from example

- Copy `frontend/.env.example` to `frontend/.env`
- Fill Firebase web config and `VITE_API_BASE_URL`

3) Run dev server

```bash
npm run dev
```

### Notes
- The app uses **Firebase Auth** (email/password).
- API requests automatically attach the Firebase ID token as `Authorization: Bearer <token>`.
- Backend default URL is `http://localhost:8080` (override with `VITE_API_BASE_URL`).

