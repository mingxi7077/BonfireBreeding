@echo off
REM 使用脚本所在目录，避免路径或编码问题
cd /d "%~dp0"
if not exist "mvnw.cmd" (
  echo Error: mvnw.cmd not found in %~dp0
  pause
  exit /b 1
)
call "%~dp0mvnw.cmd" -q -DskipTests package
if %ERRORLEVEL% equ 0 (
  echo.
  echo Build OK. JAR: %~dp0target\BonfireBreeding-1.3.3.jar
) else (
  echo Build failed. Ensure JAVA_HOME is set to JDK 17+.
)
exit /b %ERRORLEVEL%
