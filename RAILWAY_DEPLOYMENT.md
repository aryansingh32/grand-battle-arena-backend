# Railway Deployment Guide

This guide will help you deploy the Spring Boot backend to Railway with one-click GitHub deployment.

## Prerequisites

1. **GitHub Account** - Your code should be in a GitHub repository
2. **Railway Account** - Sign up at [railway.app](https://railway.app) (Free tier available)
3. **Firebase Service Account** - You'll need your Firebase credentials

---

## Step 1: Prepare Your Repository

### 1.1 Ensure These Files Are Committed

- ✅ `pom.xml`
- ✅ `src/` directory
- ✅ `railway.toml`
- ✅ `nixpacks.toml`
- ✅ `.railwayignore`

### 1.2 Important: Firebase Credentials

**Option A: Use Environment Variables (Recommended)**
- Don't commit `firebase-service-account.json` to GitHub
- We'll add it as an environment variable in Railway

**Option B: Keep in Repository (Less Secure)**
- If you must commit it, ensure it's in `src/main/resources/`

---

## Step 2: Deploy to Railway

### 2.1 Create New Project

1. Go to [railway.app](https://railway.app)
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Authorize Railway to access your GitHub
5. Select your repository
6. Railway will automatically detect it's a Java/Maven project

### 2.2 Add PostgreSQL Database

1. In your Railway project, click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. Railway will automatically create a PostgreSQL database
4. Note: Railway automatically sets `DATABASE_URL` environment variable

### 2.3 Configure Environment Variables

Go to your service → **Variables** tab and add:

#### Required Variables:

```bash
# Database (usually auto-set by Railway PostgreSQL)
DATABASE_URL=postgresql://user:password@host:port/dbname

# Firebase Configuration
FIREBASE_CONFIG_PATH=firebase-service-account.json

# Server Port (Railway sets this automatically)
PORT=8080

# Application Settings
APP_ADMIN_PASSWORD=your_secure_password_here

# Optional: Redis (if you add Redis service)
REDIS_URL=redis://:password@host:port
REDIS_HOST=host
REDIS_PORT=6379
CACHE_TYPE=redis
```

#### Optional Variables:

```bash
# Database (if not using DATABASE_URL)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=test_esport
DB_USERNAME=postgres
DB_PASSWORD=password

# Logging
DEBUG=false
SHOW_SQL=false
HIBERNATE_DDL_AUTO=update

# CORS (for frontend)
FRONTEND_ORIGIN=https://your-frontend-domain.com
```

### 2.4 Add Firebase Service Account

**Method 1: Base64 Encoded (Recommended)**

1. Convert your `firebase-service-account.json` to base64:
   ```bash
   base64 -i firebase-service-account.json
   ```

2. Add environment variable in Railway:
   - **Key:** `FIREBASE_SERVICE_ACCOUNT_BASE64`
   - **Value:** (paste the base64 string)

3. Update `FirebaseConfig.java` to decode it (see below)

**Method 2: Direct File Upload**

1. In Railway, go to your service
2. Click **"Settings"** → **"Source"**
3. Upload `firebase-service-account.json` to `src/main/resources/`
4. Commit and push to GitHub

---

## Step 3: Update Firebase Configuration (If Using Base64)

If you're using base64 encoded Firebase credentials, update `FirebaseConfig.java`:

```java
@Value("${FIREBASE_SERVICE_ACCOUNT_BASE64:}")
private String firebaseServiceAccountBase64;

@PostConstruct
public void initializeFirebase() {
    try {
        if (firebaseServiceAccountBase64 != null && !firebaseServiceAccountBase64.isEmpty()) {
            // Decode base64
            byte[] decodedBytes = Base64.getDecoder().decode(firebaseServiceAccountBase64);
            String jsonContent = new String(decodedBytes);
            
            // Initialize from JSON string
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                    new ByteArrayInputStream(jsonContent.getBytes())
                ))
                .build();
            
            FirebaseApp.initializeApp(options);
        } else {
            // Fallback to file
            FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            FirebaseApp.initializeApp(options);
        }
    } catch (Exception e) {
        log.error("Error initializing Firebase", e);
    }
}
```

---

## Step 4: Add Redis (Optional)

If you want Redis caching:

1. In Railway project, click **"+ New"**
2. Select **"Database"** → **"Add Redis"**
3. Railway will automatically set `REDIS_URL`
4. Update environment variable:
   ```bash
   CACHE_TYPE=redis
   ```

**Note:** If Redis is not available, the app will fallback to simple cache (no Redis required).

---

## Step 5: Deploy

1. Railway will automatically build and deploy when you push to GitHub
2. Or click **"Deploy"** button in Railway dashboard
3. Wait for build to complete (usually 2-5 minutes)
4. Check logs for any errors

---

## Step 6: Get Your App URL

1. After deployment, Railway will provide a URL like:
   ```
   https://your-app-name.up.railway.app
   ```

2. Update your frontend to use this URL:
   ```dart
   const String baseUrl = 'https://your-app-name.up.railway.app';
   ```

3. Update CORS in Railway environment variables:
   ```bash
   FRONTEND_ORIGIN=https://your-frontend-domain.com
   ```

---

## Step 7: Verify Deployment

### Health Check

Visit: `https://your-app-name.up.railway.app/actuator/health`

Should return:
```json
{
  "status": "UP"
}
```

### Test API

```bash
curl https://your-app-name.up.railway.app/api/public/health
```

---

## Troubleshooting

### Issue: Build Fails

**Solution:**
- Check Railway logs for Maven errors
- Ensure `pom.xml` is correct
- Verify Java version (should be 21)

### Issue: Database Connection Fails

**Solution:**
- Verify `DATABASE_URL` is set correctly
- Check PostgreSQL service is running
- Ensure database migrations run (Flyway)

### Issue: Firebase Not Working

**Solution:**
- Verify `FIREBASE_SERVICE_ACCOUNT_BASE64` is set
- Check Firebase credentials are valid
- Review logs for Firebase initialization errors

### Issue: Port Already in Use

**Solution:**
- Railway sets `PORT` automatically
- Don't hardcode port 8080
- Use `${PORT:8080}` in application.yml (already done)

### Issue: Redis Connection Fails

**Solution:**
- Redis is optional - app works without it
- Set `CACHE_TYPE=simple` to disable Redis
- Or add Redis service in Railway

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes* | - | PostgreSQL connection URL (auto-set by Railway) |
| `PORT` | No | 8080 | Server port (auto-set by Railway) |
| `FIREBASE_CONFIG_PATH` | Yes | `firebase-service-account.json` | Path to Firebase credentials |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | Yes* | - | Base64 encoded Firebase credentials |
| `APP_ADMIN_PASSWORD` | No | `admin123` | Admin password |
| `REDIS_URL` | No | - | Redis connection URL |
| `CACHE_TYPE` | No | `simple` | Cache type: `redis` or `simple` |
| `DEBUG` | No | `false` | Enable debug logging |
| `SHOW_SQL` | No | `false` | Show SQL queries |
| `FRONTEND_ORIGIN` | No | `http://localhost:3000` | CORS allowed origin |

*Either `DATABASE_URL` or individual DB variables required.

---

## Railway Free Tier Limits

- **$5 credit/month** (enough for small apps)
- **512MB RAM** per service
- **1GB storage** for databases
- **Unlimited bandwidth**

**Tips to stay within free tier:**
- Use simple cache instead of Redis (saves money)
- Optimize database queries
- Monitor usage in Railway dashboard

---

## Continuous Deployment

Railway automatically deploys when you:
1. Push to main/master branch
2. Create a new release/tag
3. Manually trigger deployment

**To disable auto-deploy:**
- Go to service → Settings → Source
- Uncheck "Auto Deploy"

---

## Monitoring

### View Logs

1. Go to Railway dashboard
2. Click on your service
3. Click **"Logs"** tab
4. Real-time logs are displayed

### Metrics

Railway provides:
- CPU usage
- Memory usage
- Network traffic
- Request count

---

## Custom Domain (Optional)

1. Go to service → Settings → Networking
2. Click **"Custom Domain"**
3. Add your domain
4. Follow DNS setup instructions

---

## Backup Database

Railway PostgreSQL includes automatic backups, but you can also:

1. Go to PostgreSQL service
2. Click **"Data"** tab
3. Click **"Download Backup"**

---

## Support

- **Railway Docs:** [docs.railway.app](https://docs.railway.app)
- **Railway Discord:** [discord.gg/railway](https://discord.gg/railway)
- **Check Logs:** Always check Railway logs first for errors

---

## Quick Deploy Checklist

- [ ] Code pushed to GitHub
- [ ] Railway project created
- [ ] PostgreSQL database added
- [ ] Environment variables set
- [ ] Firebase credentials configured
- [ ] Build successful
- [ ] Health check passes
- [ ] Frontend updated with new URL
- [ ] CORS configured
- [ ] Test API endpoints

---

**That's it! Your app should now be live on Railway! 🚀**

