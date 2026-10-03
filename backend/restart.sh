#!/bin/bash
# Reliable restart: pkill does not match Windows node processes.
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='node.exe'\" | Where-Object { \$_.CommandLine -like '*server.js*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force -ErrorAction SilentlyContinue }" >/dev/null 2>&1
sleep 2
PORT=${1:-4113} node src/server.js > ./server.log 2>&1 &
sleep 4
curl -s -o /dev/null -w "server up on ${1:-4113}: %{http_code}\n" http://localhost:${1:-4113}/health
