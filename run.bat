@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "APP_JAR=%~dp0app\massey-text-editor.jar"
set "JAVA_EXE="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for /f "delims=" %%J in ('where java.exe 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"

if not defined JAVA_EXE (
    echo [ERROR] Java was not found.
    echo Install Java 17 or newer, then run this file again.
    echo Maven and an IDE are not required.
    pause
    exit /b 1
)

if not exist "%APP_JAR%" (
    echo [ERROR] The application file is missing:
    echo "%APP_JAR%"
    echo Restore the app folder or run mvnw.cmd clean package to rebuild the project.
    pause
    exit /b 1
)

"%JAVA_EXE%" -jar "%APP_JAR%" %*
set "APP_EXIT_CODE=%ERRORLEVEL%"

if not "%APP_EXIT_CODE%"=="0" (
    echo.
    echo [ERROR] The text editor exited with code %APP_EXIT_CODE%.
    pause
)

exit /b %APP_EXIT_CODE%
