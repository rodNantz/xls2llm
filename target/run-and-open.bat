@echo off
setlocal

rem Find the highest xls2llm version built in the same directory.
set "SCRIPT_DIR=%~dp0"
set "JAR_PATH="
for /f "usebackq delims=" %%F in (`powershell -NoProfile -Command "Get-ChildItem -LiteralPath $env:SCRIPT_DIR -Filter 'xls2llm-*.jar' -File | Sort-Object { [version]($_.BaseName.Substring(8).Split('-')[0]) } -Descending | Select-Object -First 1 -ExpandProperty FullName"`) do set "JAR_PATH=%%F"

if not defined JAR_PATH (
  echo ERROR: No xls2llm JAR found in "%SCRIPT_DIR%".
  echo Build the project first with ..\mvnw.cmd package
  pause
  exit /b 1
)

echo Starting "%JAR_PATH%"...
start "xls2llm-server" cmd /k ""java" -jar "%JAR_PATH%""

echo Waiting for the server to start...
timeout /t 5 /nobreak >nul
start "" "http://localhost:8080/"

endlocal
exit /b 0
