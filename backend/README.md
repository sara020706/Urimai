# Urimai Backend (Prototype)

A minimal Express API that sits between the Urimai Android app and the Neon Postgres database. It exists because Android apps must never embed raw database credentials — this service holds the `DATABASE_URL` and exposes authenticated REST endpoints instead.

Security level: **prototype**. Passwords are bcrypt-hashed and endpoints are JWT-protected, but there's no rate limiting, refresh-token rotation, email verification, or HTTPS termination (put it behind a platform that provides HTTPS, e.g. Render/Railway/Fly.io).

## Setup

```bash
cd backend
npm install
cp .env.example .env   # then fill in DATABASE_URL and JWT_SECRET
npm run migrate        # creates tables in Postgres (safe to re-run)
npm start               # listens on PORT (default 4000)
```

## Endpoints

- `POST /auth/signup` `{ username, password, displayName }` → `{ userId, displayName, token }`
- `POST /auth/login` `{ username, password }` → `{ userId, displayName, token }`
- `GET /profile` (auth) → profile fields
- `PUT /profile` (auth) → updates and returns profile fields
- `GET /documents` (auth) → list of uploaded documents (metadata only)
- `POST /documents` (auth, multipart: `documentName`, `file`) → uploads a document, stored as `bytea` in Postgres
- `GET /documents/:id/file` (auth) → streams the raw file back
- `DELETE /documents/:id` (auth) → deletes a document
- `GET /saved-schemes` / `PUT /saved-schemes/:schemeId` / `DELETE /saved-schemes/:schemeId` (auth)

All authenticated routes expect `Authorization: Bearer <token>`.

## Pointing the Android app at this backend

`app/src/main/java/com/example/data/remote/ApiClient.kt` has a `BASE_URL` constant:

- Running the backend on your machine + testing on the Android **emulator**: leave it as `http://10.0.2.2:4000/` (the emulator's alias for your host machine's localhost).
- Testing on a **physical device** on the same network: use your machine's LAN IP, e.g. `http://192.168.1.23:4000/`.
- Using a **deployed** backend (Render/Railway/etc.): use its HTTPS URL, e.g. `https://urimai-backend.onrender.com/`.

If you switch away from `10.0.2.2` (plain HTTP) to a real HTTPS host, no manifest changes are needed — cleartext HTTP is only reachable today because no network security config blocks it, which is fine for prototype/local dev.
