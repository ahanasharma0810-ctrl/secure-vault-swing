@echo off
REM Builds the app if needed and starts it.
cd /d "%~dp0"
if not exist secure-vault.jar call build.bat
if errorlevel 1 exit /b 1
start "" javaw -jar secure-vault.jar
