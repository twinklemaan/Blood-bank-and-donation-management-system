# Render & MongoDB Atlas Deployment Guide (Unit 6)

This guide documents the exact steps to deploy the Blood Bank full-stack application to the cloud using MongoDB Atlas (database) and Render (hosting).

---

## 1. MongoDB Atlas Setup (Free M0 Cluster)

1. Sign up or log in at [https://www.mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas).
2. Click **Create a Deployment** and select the **M0 Free Tier** (AWS or GCP, choose closest region e.g. Mumbai).
3. **Database Access (User Setup):**
   - Under **Security > Database Access**, click **Add New Database User**.
   - Authentication Method: **Password**.
   - Username: `bloodbank_admin`
   - Password: Choose a secure password (e.g., `SecureAtlasPass123`).
   - Role: `Read and write to any database`.
4. **Network Access (IP Whitelist):**
   - Under **Security > Network Access**, click **Add IP Address**.
   - Click **Allow Access from Anywhere** (`0.0.0.0/0`) so Render servers can connect.
   - Confirm and save.
5. **Get Connection String:**
   - On the Overview dashboard, click **Connect > Drivers > Node.js**.
   - Copy the SRV URI:
     ```
     mongodb+srv://bloodbank_admin:<password>@cluster0.xxxxx.mongodb.net/blood_bank?retryWrites=true&w=majority
     ```
   - Replace `<password>` with your database user password.

---

## 2. Seed MongoDB Atlas Remotely

Before starting the web app, run the seed script locally against Atlas to populate the initial collections:
```bash
cd backend
# Temporarily set MONGODB_URI to your Atlas connection string in backend/.env:
# MONGODB_URI="mongodb+srv://bloodbank_admin:SecureAtlasPass123@cluster0.xxxxx.mongodb.net/blood_bank"
npm run seed
```
Verify that Atlas displays the collections: `users`, `banks`, `donors`, `donations`, `bloodrequests`.

---

## 3. GitHub Repository Sync

Ensure all local commits are pushed to your GitHub repository:
```bash
git checkout main
git merge develop
git push origin main
```

---

## 4. Render Deployment via Blueprint (`render.yaml`)

1. Log in to [https://render.com](https://render.com).
2. In the Render Dashboard, click **New + > Blueprint**.
3. Connect your GitHub repository: `Blood-bank-and-donation-management-system`.
4. Branch: Select `main`.
5. Render detects `render.yaml` and creates three services:
   - `bloodbank-api` (Web Service)
   - `bloodbank-portal` (Static Site for React)
   - `bloodbank-static` (Static Site for HTML/CSS site)

### Setting Environment Variables in Render Dashboard

1. Navigate to **bloodbank-api > Environment**:
   - `MONGODB_URI`: `<Your MongoDB Atlas SRV URI>`
   - `JWT_SECRET`: `my_production_jwt_secret_key_987`
   - `PORT`: `5000`
   - `CORS_ORIGIN`: `https://bloodbank-portal.onrender.com` (your frontend static site URL)

2. Navigate to **bloodbank-portal > Environment**:
   - `VITE_API_URL`: `https://bloodbank-api.onrender.com` (your backend web service URL)

3. Trigger **Manual Deploy > Deploy latest commit** on both services.

---

## 5. Verify Live Deployment

1. Visit the backend health check:
   `https://bloodbank-api.onrender.com/api/health` -> Expect `{ "status": "OK" }`
2. Open the React frontend:
   `https://bloodbank-portal.onrender.com`
3. Log in with demo credentials:
   - Staff: `staff1` / `staff123`
   - Hospital: `hosp1` / `hosp123`

---

## 6. Common Problems & Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| **Free instance cold start (50s delay)** | Render spins down free tier web services after 15 min of inactivity. | Normal on free plan; wait 30–60 seconds on first request or ping `/api/health`. |
| **CORS Error in browser console** | `CORS_ORIGIN` on backend does not match frontend Render domain. | In Render backend environment, set `CORS_ORIGIN` to exact frontend URL (no trailing slash). |
| **MongoServerSelectionError** | MongoDB Atlas IP Access List blocked Render. | In Atlas > Network Access, ensure `0.0.0.0/0` (Allow anywhere) is enabled. |
| **Wrong Root Directory** | Render build fails looking for `package.json`. | Ensure `rootDir` is set to `backend` for API and `frontend` for React portal. |
| **Uploaded files disappear** | Render free tier instances use ephemeral disks. | Expected behavior for demo; files persist during the active session. |
| **Email not arriving** | Render free instances may restrict outbound port 25/587. | Expected for free hosting; our mailer catches errors gracefully without crashing the API. |
