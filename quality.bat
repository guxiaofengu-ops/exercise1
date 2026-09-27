@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd clean verify
exit /b %ERRORLEVEL%
