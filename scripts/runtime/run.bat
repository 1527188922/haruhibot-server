@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA_EXE=%~dp0jre\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -Xms512m -Xmx512m -Xss256k -XX:+UseG1GC -jar haruhibotServer.jar
pause