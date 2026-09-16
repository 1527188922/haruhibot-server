@echo off
chcp 65001

cd /d %~dp0/webui
call npm run build

cd /d %~dp0
call mvn clean package -U -DskipTests
pause