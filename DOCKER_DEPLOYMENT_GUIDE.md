# 🐳 Docker Deployment Guide for Railway

Complete step-by-step guide to deploy your Spring Boot backend to Railway using Docker.

---

## 📋 Section A: Project Analysis Summary

### Project Overview
- **Build System:** Maven 3.x
- **Java Version:** 21 (Eclipse Temurin)
- **Spring Boot Version:** 3.5.4
- **Application Name:** EsportTournament
- **JAR Artifact:** `EsportTournament-0.0.1-SNAPSHOT.jar`
- **Default Port:** 8080 (configurable via `PORT` env var)

### Dependencies & Services
- ✅ **PostgreSQL** - Database (required)
- ✅ **Redis** - Caching (optional, falls back to simple cache)
- ✅ **Firebase Admin SDK** - Authentication & Notifications (required)
- ✅ **Flyway** - Database migrations (auto-runs on startup)

### Runtime Requirements
- **Port:** Uses `${PORT}` environment variable (Railway sets this automatically)
- **Database:** Requires `DATABASE_URL` or individual DB connection variables
- **Firebase:** Requires `FIREBASE_SERVICE_ACCOUNT_BASE64` (base64 encoded JSON) or file in classpath
- **Memory:** Recommended minimum 512MB, optimal 1GB+

### Key Configuration Files
- `application.yml` - Main configuration (supports env vars)
- `pom.xml` - Maven build configuration
- `src/main/resources/db/migration/` - Flyway migration scripts

---

## 📦 Section B: Dockerfile

The production-ready Dockerfile uses a multi-stage build:

1. **Builder Stage:** Maven builds the JAR
2. **Runtime Stage:** Lightweight Alpine-based JRE image runs the application

**Location:** `Dockerfile` (root directory)

**Key Features:**
- ✅ Multi-stage build for smaller final image
- ✅ Layer caching for faster rebuilds
- ✅ Non-root user for security
- ✅ JVM optimizations for containers
- ✅ Health check endpoint
- ✅ Alpine Linux base (minimal size)

---

## 🚫 Section C: .dockerignore

The `.dockerignore` file ensures only necessary files are included in the Docker build context:

**Excluded:**
- Build artifacts (`target/`, `*.jar`)
- IDE files (`.idea/`, `.vscode/`)
- Logs and temporary files
- Git files
- Documentation files
- Test files

**Result:** Faster builds, smaller context, cleaner images

---

## 🚀 Section D: Step-by-Step Deployment Guide

### Step 1: Prepare the Repository

#### 1.1 Commit Required Files

Ensure these files are committed to your Git repository:

```bash
# Required files
✅ Dockerfile
✅ .dockerignore
✅ pom.xml
✅ src/ (entire directory)
✅ .mvn/ (Maven wrapper)
✅ mvnw (Maven wrapper script)
```

#### 1.2 Files to NOT Commit (Security)

**Never commit these:**
- ❌ `firebase-service-account.json` (use environment variable instead)
- ❌ `.env` files with secrets
- ❌ `target/` directory (build artifacts)
- ❌ `logs/` directory

#### 1.3 Verify Local Build

Test that your Dockerfile works locally:

```bash
# Build the Docker image
docker build -t esport-tournament:latest .

# Run locally (optional test)
docker run -p 8080:8080 \
  -e PORT=8080 \
  -e DATABASE_URL=postgresql://user:pass@host:5432/dbname \
  -e FIREBASE_SERVICE_ACCOUNT_BASE64="<your-base64-string>" \
  esport-tournament:latest
```

**Expected output:** Application starts successfully, logs show Spring Boot banner.

#### 1.4 Push to GitHub

```bash
git add Dockerfile .dockerignore
git commit -m "Add Dockerfile for Railway deployment"
git push origin main
```

---

### Step 2: Deploy on Railway with Docker

#### 2.1 Create Railway Account

1. Go to [railway.app](https://railway.app)
2. Sign up or log in (GitHub OAuth recommended)

#### 2.2 Create New Project

1. Click **"New Project"** button (top right)
2. Select **"Deploy from GitHub repo"**
3. Authorize Railway to access your GitHub account (if first time)
4. Select your repository: `grand-battle-arena-backend`

#### 2.3 Configure Build Settings (IMPORTANT)

**Railway will auto-detect Nixpacks. You need to switch to Docker:**

1. In your Railway project, click on your **service** (the backend service)
2. Go to **Settings** tab
3. Scroll to **"Build & Deploy"** section
4. Find **"Build Command"** or **"Dockerfile Path"**
5. **Enable Docker:**
   - Set **"Build Command"** to: `docker build -t app .`
   - OR set **"Dockerfile Path"** to: `Dockerfile`
   - OR in **Settings → Source**, change **"Build Type"** from `Nixpacks` to `Dockerfile`

**Alternative Method:**
- Railway may auto-detect `Dockerfile` in root
- If it doesn't, go to **Settings → Source → Build Type** → Select **"Dockerfile"**

#### 2.4 Add PostgreSQL Database

1. In your Railway project dashboard, click **"+ New"** button
2. Select **"Database"** → **"Add PostgreSQL"**
3. Railway automatically:
   - Creates a PostgreSQL database
   - Sets `DATABASE_URL` environment variable
   - Links it to your service

**Note:** The `DATABASE_URL` format is:
```
postgresql://user:password@hostname:port/railway
```

#### 2.5 Configure Environment Variables

Go to your service → **Variables** tab and add:

##### Required Variables:

```bash
# Database (usually auto-set by Railway PostgreSQL service)
# DATABASE_URL is automatically set - DO NOT override unless needed

# Firebase Configuration (REQUIRED)
FIREBASE_SERVICE_ACCOUNT_BASE64=<your-base64-encoded-json>

# Server Port (Railway sets this automatically)
# PORT is auto-set by Railway - DO NOT manually set

# Application Admin Password
APP_ADMIN_PASSWORD=your_secure_password_here
```

##### Optional Variables:

```bash
# Redis (if you add Redis service)
REDIS_URL=redis://:password@host:port
REDIS_HOST=hostname
REDIS_PORT=6379
CACHE_TYPE=redis

# Database (if not using DATABASE_URL)
DB_HOST=hostname
DB_PORT=5432
DB_NAME=railway
DB_USERNAME=postgres
DB_PASSWORD=password

# Logging & Debug
DEBUG=false
SHOW_SQL=false
HIBERNATE_DDL_AUTO=update

# CORS (for frontend)
FRONTEND_ORIGIN=https://your-frontend-domain.com
```

##### How to Get Firebase Base64:

```bash
# On your local machine
base64 -i firebase-service-account.json

# Or on macOS
base64 -i firebase-service-account.json | pbcopy

# Copy the entire output and paste as FIREBASE_SERVICE_ACCOUNT_BASE64 value
```

#### 2.6 Monitor Build Process

1. Go to **Deployments** tab in Railway
2. Watch the build logs in real-time
3. You should see:
   - Docker build starting
   - Maven downloading dependencies
   - Maven building JAR
   - Docker image creation
   - Container starting

**Expected build time:** 3-5 minutes (first build), 1-2 minutes (subsequent builds with cache)

#### 2.7 Verify Deployment

Once deployment completes:

1. Railway provides a public URL like: `https://your-app-name.up.railway.app`
2. Check **Logs** tab for application startup
3. Look for: `Started EsportTournamentApplication` in logs

#### 2.8 Test Health Endpoint

```bash
# Test health check
curl https://your-app-name.up.railway.app/actuator/health

# Expected response:
# {"status":"UP"}
```

---

### Step 3: Post-Deployment Instructions

#### 3.1 Port Configuration

✅ **Already Configured!** Your `application.yml` uses:
```yaml
server:
  port: ${PORT:8080}
```

Railway automatically sets the `PORT` environment variable. **No action needed.**

#### 3.2 JVM Memory Configuration

Railway sets memory limits automatically. To optimize JVM:

**Option 1: Use Railway's Memory Settings**
- Go to **Settings** → **Resources**
- Set **Memory Limit** (recommended: 1GB for production)

**Option 2: Custom JVM Flags (if needed)**
Add environment variable:
```bash
JAVA_OPTS=-Xmx512m -Xms256m
```

**Note:** The Dockerfile already includes optimized JVM flags for containers.

#### 3.3 Check Health & Uptime

**Health Check Endpoint:**
```
https://your-app-name.up.railway.app/actuator/health
```

**Monitor in Railway:**
1. Go to **Metrics** tab → View CPU, Memory, Network
2. Go to **Logs** tab → View real-time application logs
3. Go to **Deployments** tab → View deployment history

#### 3.4 Trigger Redeploys

**Automatic Redeploys:**
- Railway auto-deploys on every push to your main branch (if enabled)

**Manual Redeploy:**
1. Go to **Deployments** tab
2. Click **"Redeploy"** on the latest deployment
3. Or push a new commit to trigger auto-deploy

**Redeploy with Environment Variable Changes:**
- Update variables in **Variables** tab
- Railway automatically redeploys

---

## 🔧 Section E: Troubleshooting Notes

### Issue 1: Build Fails - "Cannot find Dockerfile"

**Symptoms:**
- Error: `Cannot locate Dockerfile`
- Build uses Nixpacks instead of Docker

**Solution:**
1. Verify `Dockerfile` is in repository root
2. Go to **Settings → Source → Build Type** → Select **"Dockerfile"**
3. Or add `railway.toml` with:
   ```toml
   [build]
   builder = "DOCKERFILE"
   dockerfilePath = "Dockerfile"
   ```

### Issue 2: Build Fails - Maven Dependencies

**Symptoms:**
- `Failed to download dependency`
- `Maven build failed`

**Solution:**
1. Check `pom.xml` is valid: `mvn validate`
2. Verify internet connectivity in Railway build logs
3. Check Maven repository access
4. Try clearing Railway build cache (Settings → Clear Cache)

### Issue 3: Application Fails to Start - Database Connection

**Symptoms:**
- `Connection refused` errors
- `Database does not exist`

**Solution:**
1. Verify PostgreSQL service is running in Railway
2. Check `DATABASE_URL` is set (should be auto-set)
3. Verify database service is linked to your app service
4. Check database credentials in Railway PostgreSQL service

### Issue 4: Application Fails to Start - Firebase

**Symptoms:**
- `Firebase initialization failed`
- `Invalid Firebase credentials`

**Solution:**
1. Verify `FIREBASE_SERVICE_ACCOUNT_BASE64` is set correctly
2. Test base64 encoding: `echo $FIREBASE_SERVICE_ACCOUNT_BASE64 | base64 -d | jq .`
3. Ensure no newlines or spaces in the base64 string
4. Check Firebase service account JSON is valid

### Issue 5: Port Already in Use

**Symptoms:**
- `Port 8080 already in use`
- Application won't start

**Solution:**
✅ **Already Fixed!** Your app uses `${PORT}` which Railway sets automatically. If you see this error:
1. Remove any hardcoded `PORT=8080` from environment variables
2. Let Railway set it automatically
3. Verify `application.yml` uses `${PORT:8080}`

### Issue 6: Out of Memory Errors

**Symptoms:**
- `OutOfMemoryError`
- Container killed

**Solution:**
1. Increase memory limit: **Settings → Resources → Memory Limit** (set to 1GB+)
2. Adjust JVM flags: Add `JAVA_OPTS=-Xmx768m -Xms256m`
3. Check for memory leaks in application code

### Issue 7: Slow Build Times

**Symptoms:**
- Builds take 10+ minutes

**Solution:**
1. Railway caches Docker layers automatically
2. Ensure `.dockerignore` excludes unnecessary files
3. Verify Maven dependency caching in Dockerfile (already implemented)
4. Consider using Railway's build cache feature

### Issue 8: Health Check Fails

**Symptoms:**
- `/actuator/health` returns 404 or timeout

**Solution:**
1. Verify Spring Boot Actuator is in dependencies (already in `pom.xml`)
2. Check `application.yml` has actuator endpoints enabled (already configured)
3. Verify application started successfully (check logs)
4. Test health endpoint manually: `curl https://your-app.up.railway.app/actuator/health`

### Issue 9: Flyway Migrations Fail

**Symptoms:**
- `Migration failed`
- Database schema errors

**Solution:**
1. Check database connection is working
2. Verify Flyway migration files in `src/main/resources/db/migration/`
3. Check migration file naming: `V{version}__{description}.sql`
4. Review Flyway logs in application startup logs
5. If needed, set `FLYWAY_ENABLED=false` temporarily to debug

### Issue 10: Redis Connection Issues

**Symptoms:**
- `Redis connection failed`
- Cache errors

**Solution:**
1. Redis is **optional** - app works without it
2. Set `CACHE_TYPE=simple` to disable Redis
3. Or add Redis service in Railway and set `REDIS_URL`
4. Verify Redis credentials if using external Redis

---

## 📊 Quick Reference

### Environment Variables Checklist

| Variable | Required | Auto-Set | Description |
|----------|----------|----------|-------------|
| `PORT` | ✅ | ✅ | Server port (Railway sets automatically) |
| `DATABASE_URL` | ✅ | ✅ | PostgreSQL connection (Railway sets automatically) |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | ✅ | ❌ | Base64 encoded Firebase JSON |
| `APP_ADMIN_PASSWORD` | ⚠️ | ❌ | Admin password (default: admin123) |
| `REDIS_URL` | ❌ | ❌ | Redis connection (optional) |
| `CACHE_TYPE` | ❌ | ❌ | Cache type: `redis` or `simple` |

### Railway URLs

- **Dashboard:** [railway.app](https://railway.app)
- **Documentation:** [docs.railway.app](https://docs.railway.app)
- **Support:** [discord.gg/railway](https://discord.gg/railway)

### Useful Commands

```bash
# Test Docker build locally
docker build -t esport-tournament .

# Test Docker run locally
docker run -p 8080:8080 \
  -e PORT=8080 \
  -e DATABASE_URL="postgresql://..." \
  -e FIREBASE_SERVICE_ACCOUNT_BASE64="..." \
  esport-tournament

# Check Docker image size
docker images esport-tournament

# View Railway logs (via CLI)
railway logs
```

---

## ✅ Deployment Checklist

Before going live, verify:

- [ ] Dockerfile builds successfully locally
- [ ] `.dockerignore` excludes unnecessary files
- [ ] All environment variables are set in Railway
- [ ] PostgreSQL database is created and linked
- [ ] Firebase credentials are configured (base64)
- [ ] Health endpoint responds: `/actuator/health`
- [ ] Application logs show successful startup
- [ ] Database migrations completed (check Flyway logs)
- [ ] Public URL is accessible
- [ ] CORS is configured (if using frontend)

---

## 🎉 Success!

Your Spring Boot application is now deployed on Railway using Docker!

**Next Steps:**
1. Update your frontend to use the Railway URL
2. Configure custom domain (optional, in Railway Settings)
3. Set up monitoring and alerts
4. Enable automatic deployments from GitHub

---

**Last Updated:** Generated for EsportTournament v0.0.1-SNAPSHOT  
**Spring Boot:** 3.5.4  
**Java:** 21 (Eclipse Temurin)

