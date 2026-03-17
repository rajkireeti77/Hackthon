@echo off
set DISABLE_AUTH=true
set SANDBOX_SCANNER_ENABLED=true
set SANDBOX_SCANNER_BASE_URL=http://localhost:8002
set WINDOWS_NOTIFICATIONS_ENABLED=true
set WINDOWS_NOTIFICATIONS_USER_ID=demo
java -jar target\phishguard-ai-backend-0.1.0.jar >> ..\run-logs\backend-auto.log 2>> ..\run-logs\backend-auto.err
