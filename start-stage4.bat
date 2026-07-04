@echo off
echo ============================================
echo  Stage 4: 实时热门影视监控大屏
echo  Port: 8080
echo  URL: http://localhost:8080/dashboard.html
echo ============================================
cd /d "%~dp0stage4-standalone"
mvn spring-boot:run
pause