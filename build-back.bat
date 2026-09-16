@echo off
chcp 65001

call mvn clean package -U -DskipTests
pause