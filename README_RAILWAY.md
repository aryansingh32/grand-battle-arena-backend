# 🚂 Railway Deployment - Quick Start

## One-Click Deploy to Railway

Your Spring Boot backend is now Railway-ready! Just follow these steps:

### 1. Push to GitHub
```bash
git add .
git commit -m "Railway deployment ready"
git push origin main
```

### 2. Deploy on Railway

1. Go to [railway.app](https://railway.app) and sign up/login
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select your repository
4. Railway will auto-detect Java/Maven and start building

### 3. Add PostgreSQL Database

1. In Railway project, click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
2. Railway automatically sets `DATABASE_URL` environment variable

### 4. Set Environment Variables

Go to your service → **Variables** and add:

```bash
# Firebase (Required)
FIREBASE_SERVICE_ACCOUNT_BASE64=<base64_encoded_firebase_json>

# Optional
APP_ADMIN_PASSWORD=your_secure_password
CACHE_TYPE=simple  # Use 'redis' if you add Redis service
```

**To get Firebase base64:**
```bash
base64 -i firebase-service-account.json
# Copy the output and paste as FIREBASE_SERVICE_ACCOUNT_BASE64
```

### 5. Deploy!

Railway will automatically:
- ✅ Build your Maven project
- ✅ Run Flyway migrations
- ✅ Start your Spring Boot app
- ✅ Provide a public URL

### 6. Get Your URL

After deployment, Railway gives you a URL like:
```
https://your-app-name.up.railway.app
```

Update your Flutter app to use this URL!

---

## What's Configured?

✅ **PostgreSQL** - Auto-configured from Railway's `DATABASE_URL`  
✅ **Port** - Auto-detected from Railway's `PORT` variable  
✅ **Firebase** - Supports base64 encoded credentials  
✅ **Redis** - Optional, falls back to simple cache if not available  
✅ **Flyway** - Auto-runs migrations on startup  
✅ **Health Check** - Available at `/actuator/health`  

---

## Need Help?

See [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md) for detailed guide.

---

**That's it! Your app is live! 🎉**

