# Apache Tomcat Servlet Deployment Guide (Unit 5)

This guide documents how to package, deploy, run, and debug the Java Servlet application on Apache Tomcat (8.5 / 9.0).

---

## 1. Prerequisites
- **Apache Tomcat 8.5 or 9.0** downloaded and unzipped (e.g. to `C:\apache-tomcat-9.0.xx`).
- **Java Development Kit (JDK 8 or higher)**.
- **Apache Maven (3.8+)**.
- The Node.js Backend API running on `http://localhost:5000`.

---

## 2. Step 1: Build the WAR File

From the root of the project:
```bash
cd tomcat-app
mvn clean package
```
This generates the packaged Web Application Archive at:
`tomcat-app/target/stock-app.war`

---

## 3. Step 2: Deploy to Tomcat

1. Copy `stock-app.war` into your Tomcat `webapps` directory:
   ```bash
   # Windows PowerShell example:
   Copy-Item "tomcat-app\target\stock-app.war" -Destination "C:\apache-tomcat-9.0.xx\webapps\"
   ```

2. Start the Tomcat server:
   - On Windows: Run `C:\apache-tomcat-9.0.xx\bin\startup.bat`
   - On Linux/macOS: Run `catalina.sh start`

3. Tomcat will automatically extract `stock-app.war` into a folder called `stock-app` inside `webapps`.

---

## 4. Step 3: Open the Servlet in Browser

1. Ensure the Node.js backend is active:
   ```bash
   cd backend
   node src/server.js
   ```

2. Open the following URL in your web browser:
   `http://localhost:8080/stock-app/stock`

3. You will see an HTML table rendering the real-time blood stock aggregated from MongoDB via the Node.js REST API.

---

## 5. Step 4: Debugging & Troubleshooting

### Scenario A: "API not reachable" message displayed in red
- **Cause:** The Node.js server is stopped or port 5000 is blocked.
- **Fix:** Start the backend with `npm start` inside `backend/` and verify `http://localhost:5000/api/public/stock` returns JSON.
- **Custom Port:** You can set the API host dynamically via Java system property in `catalina.bat`:
  `-Dapi.base=http://127.0.0.1:5000`

### Scenario B: 404 Not Found on Tomcat
- Verify `stock-app.war` is in `webapps/`.
- Check `web.xml` exists under `webapps/stock-app/WEB-INF/web.xml`.
- Ensure you navigate to `/stock-app/stock` (note context path matches WAR filename).

### Scenario C: Viewing Tomcat Logs
Check the runtime logs to trace servlet initialization:
`C:\apache-tomcat-9.0.xx\logs\catalina.out` or `localhost.<date>.log`.
