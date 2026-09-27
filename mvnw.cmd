@echo off
setlocal
set MAVEN_VERSION=3.9.11
set BASE_DIR=%~dp0
set CACHE_DIR=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
set ARCHIVE=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.tar.gz
set URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.tar.gz

if not exist "%CACHE_DIR%\bin\mvn.cmd" (
  if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"
  echo Preparing the project Maven %MAVEN_VERSION% runtime...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%URL%' -OutFile '%ARCHIVE%'"
  if errorlevel 1 exit /b 1
  tar -xzf "%ARCHIVE%" -C "%BASE_DIR%.mvn"
  if errorlevel 1 exit /b 1
  del /q "%ARCHIVE%"
)

call "%CACHE_DIR%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
