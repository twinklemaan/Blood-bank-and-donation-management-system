@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

if not exist "out" mkdir "out"

echo [BUILD] Compiling Java sources...
javac -encoding UTF-8 -cp "lib/*" -d out (dir /s /b src\*.java)

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Build succeeded!

if "%1"=="run" (
    echo [RUN] Launching Blood Bank Application...
    java -cp "out;lib/*" com.bloodbank.Main
)
