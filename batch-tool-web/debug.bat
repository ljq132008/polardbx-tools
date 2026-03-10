@echo off
REM Debug 模式启动脚本 (Windows)

echo ========================================
echo   Batch Tool Web - Debug Mode
echo ========================================
echo.
echo Debug Port: 5005
echo Service URL: http://localhost:8080
echo.

REM 先构建项目
echo [1/2] Building project...
call mvn clean package -DskipTests

REM Debug 模式启动
echo [2/2] Starting in debug mode...
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar target/batch-tool-web-1.0.0.jar

pause
