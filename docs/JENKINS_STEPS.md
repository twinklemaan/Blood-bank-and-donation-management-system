# Jenkins CI/CD Setup Guide (Unit 5)

This document provides clear, step-by-step instructions for demonstrating Continuous Integration using Jenkins for the Blood Bank project.

---

## 1. Local Jenkins Installation & Launch

### Option A: Running the Jenkins WAR (Easiest)
1. Download the latest Jenkins WAR from [https://www.jenkins.io/download/](https://www.jenkins.io/download/).
2. Run Jenkins from PowerShell / Terminal:
   ```bash
   java -jar jenkins.war --httpPort=8080
   ```
3. Open your browser at `http://localhost:8080`.
4. Copy the initial administrator password from the console or from:
   - Windows: `C:\Users\<YourUser>\.jenkins\secrets\initialAdminPassword`
5. Select **Install Suggested Plugins**.
6. Create an admin user (e.g., `admin` / `admin123`).

---

## 2. Prerequisites Inside Jenkins
Ensure the following tools are installed and accessible on your system PATH:
- **Node.js (v18+)** and **npm**
- **Git**
- **Maven (v3.8+)**
- **JDK (Java 8 or higher)**

In Jenkins under **Manage Jenkins > Tools**, you can verify Git, Maven, and JDK installations.

---

## 3. Creating the Pipeline Job

1. On the Jenkins dashboard, click **New Item**.
2. Enter Item Name: `BloodBank-Pipeline`.
3. Select **Pipeline** as the project type and click **OK**.
4. In the job configuration:
   - Scroll down to the **Pipeline** section.
   - Set **Definition**: `Pipeline script from SCM`.
   - Set **SCM**: `Git`.
   - **Repository URL**: `https://github.com/twinklemaan/Blood-bank-and-donation-management-system.git` (or your local file/git path).
   - **Branch Specifier**: `*/develop` (or `*/main`).
   - **Script Path**: `Jenkinsfile`.
5. Click **Save**.

---

## 4. Running the Pipeline & Verifying Green Build

1. Click **Build Now** on the left menu.
2. Monitor the build progress under **Build History**.
3. Click on the active build number (e.g. `#1`) -> **Console Output** to observe:
   - Stage 1: `Backend Install` (`npm ci`)
   - Stage 2: `Frontend Install and Build` (`npm ci` & `npm run build`)
   - Stage 3: `Tomcat App Package` (`mvn clean package`)
4. Verify all 3 stages show green in the **Stage View**.
5. **Viva Rule:** Only push local commits to the remote `main` branch when the develop pipeline build is completely green!

---

## 5. Troubleshooting Windows Jenkins Runners
If running on Windows and Jenkins complains that `sh` is not found:
Edit `Jenkinsfile` and replace `sh` with `bat`:
```groovy
stage('Backend Install') {
    steps {
        dir('backend') {
            bat 'npm ci'
        }
    }
}
```
