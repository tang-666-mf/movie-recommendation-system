@echo off
echo ============================================
echo  Stage 5: 个性化电影推荐系统
echo  Port: 8081
echo  URL: http://localhost:8081/recommend.html
echo ============================================
cd /d "%~dp0stage5-standalone"
mvn spring-boot:run
pause