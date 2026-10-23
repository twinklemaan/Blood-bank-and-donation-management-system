# Git Version Control Guide (Unit 5)

This guide documents the essential Git commands and workflow used in this full-stack project, suitable for demonstration in a viva examination.

---

## 1. Branching Strategy

We follow a clean, industry-standard branching model:
- `main`: Production-ready, stable releases.
- `develop`: Ongoing development and feature integration.

---

## 2. Essential Git Workflow & Commands

### 2.1 Repository Setup & Cloning
```bash
# Initialize a new Git repository locally (if starting fresh)
git init

# Or clone the remote repository from GitHub
git clone https://github.com/twinklemaan/Blood-bank-and-donation-management-system.git
cd BloodBankApp
```

### 2.2 Branch Management
```bash
# List all local branches (* marks the active branch)
git branch

# List all branches including remote tracking branches
git branch -a

# Create and switch to a new branch called 'develop'
git checkout -b develop

# Switch between existing branches
git checkout main
git checkout develop
```

### 2.3 Staging and Committing Changes
```bash
# Check working tree status (untracked, modified, staged files)
git status

# Stage specific files
git add backend/src/server.js

# Stage all modified and new files (respecting .gitignore)
git add .

# Record staged changes to repository history with a clear message
git commit -m "feat(backend): implement auth and donor CRUD routes"
```

### 2.4 Synchronizing with GitHub Remote
```bash
# Verify configured remote repositories
git remote -v

# Fetch the latest commits and branches from GitHub without merging
git fetch origin

# Pull the latest changes from the remote develop branch into current branch
git pull origin develop

# Push local commits to the remote develop branch
git push -u origin develop
```

### 2.5 Merging Features into Main
```bash
# Switch to main branch
git checkout main

# Ensure local main is up to date
git pull origin main

# Merge develop into main
git merge develop

# Push the updated main branch to GitHub
git push origin main
```

---

## 3. How Local Copy Syncs with GitHub Remote
1. **Working Directory & Staging Area:** Local edits occur in files on disk. `git add` stages changes into index.
2. **Local Repository:** `git commit` moves staged changes to the local repository history (`.git`).
3. **Remote Repository (GitHub):** `git push` transmits local commits across HTTPS to GitHub's remote repository.
4. **Collaboration & Updates:** When collaborators or CI/CD update GitHub, `git fetch` downloads references, and `git merge` or `git pull` updates the local working tree.
