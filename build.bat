@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

if not exist "out" mkdir "out"

echo [BUILD] Finding Java source files...
dir /s /b src\*.java > sources.txt

echo [BUILD] Compiling Java sources...
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
del sources.txt

if %COMPILE_STATUS% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %COMPILE_STATUS%
)

echo [SUCCESS] Build succeeded!

if "%1"=="run" (
    echo [RUN] Launching Blood Bank Application...
    java -cp "out;lib/*" com.bloodbank.Main
)
