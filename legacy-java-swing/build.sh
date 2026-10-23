#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p out

echo "[BUILD] Compiling Java sources..."
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt
rm -f sources.txt

echo "[SUCCESS] Build succeeded!"

if [ "$1" = "run" ]; then
    echo "[RUN] Launching Blood Bank Application..."
    java -cp "out:lib/*" com.bloodbank.Main
fi
