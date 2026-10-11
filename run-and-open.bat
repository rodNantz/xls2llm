@echo off
rem Run the xls2llm jar in a new console window and open the browser to the web UI
setlocal
:: Determine script directory and jar path
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%xls2llm-0.0.1-SNAPSHOT.jar

if not exist "%JAR_PATH%" (
  echo ERROR: Jar not found: %JAR_PATH%
  echo Make sure you run this script from the project root where the jar is located.
  pause
  exit /b 1
)
echo Starting xls2llm server...
rem Start server in a new window (keeps the window open)
start "xls2llm-server" cmd /k "java -jar "%JAR_PATH%""
echo Waiting a few seconds for the server to start...
timeout /t 5 /nobreak >nul
echo Opening http://localhost:8080/ in your default browser...
start "" "http://localhost:8080/"
echo Done. Server output is in the new console window.
endlocal
exit /b 0
