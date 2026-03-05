# Production Deployment Ready Guide

## 1. One-click local production stack (Docker)

```bash
cd grand-battle-arena-backend
cp .env.example .env
# fill FIREBASE_SERVICE_ACCOUNT_BASE64 and APP_ADMIN_PASSWORD in .env
docker compose up -d --build
```

Health check:

```bash
curl http://localhost:10000/actuator/health
```

Stop:

```bash
docker compose down
```

Stop + remove data:

```bash
docker compose down -v
```

## 2. Render deployment

This repo already contains `render.yaml`.

Required env vars on Render:

1. `APP_ADMIN_PASSWORD` (strong password)
2. `FIREBASE_SERVICE_ACCOUNT_BASE64` (recommended)
3. `ALLOWED_ORIGIN_PATTERNS` (your admin/mobile/web domains)

Notes:

1. Render gives `DATABASE_URL` and `REDIS_URL`.
2. `docker-entrypoint.sh` auto-converts `DATABASE_URL` to JDBC URL.
3. `CACHE_TYPE=redis` is already set in `render.yaml`.

## 3. Railway deployment

Use Dockerfile deployment.

Required Railway env vars:

1. `DATABASE_URL` (from Railway Postgres)
2. `REDIS_URL` (from Railway Redis)
3. `CACHE_TYPE=redis`
4. `APP_ADMIN_PASSWORD`
5. `FIREBASE_SERVICE_ACCOUNT_BASE64`
6. `ALLOWED_ORIGIN_PATTERNS`

`docker-entrypoint.sh` handles `DATABASE_URL` conversion automatically.

## 4. Firebase credentials (important)

Generate service account JSON from Firebase console:

1. Firebase Console -> Project Settings -> Service Accounts
2. Generate new private key
3. Download JSON

Recommended secure way for cloud:

```bash
base64 -w 0 firebase-service-account.json
```

Paste output into env var:

1. `FIREBASE_SERVICE_ACCOUNT_BASE64=<base64_string>`

Alternative options supported:

1. `FIREBASE_CREDENTIALS` (raw JSON string)
2. `FIREBASE_SERVICE_ACCOUNT_FILE` (classpath file path inside container/app)

## 5. Production checklist

1. Set `APP_ADMIN_PASSWORD` to strong random value
2. Set exact `ALLOWED_ORIGIN_PATTERNS` (no wildcard)
3. Ensure Postgres backups enabled on platform
4. Ensure Redis persistence enabled (or managed Redis with SLA)
5. Verify `/actuator/health` is green after deploy
6. Test login + tournament list + deposit flow + push notifications

