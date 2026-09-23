@echo off
setlocal
pushd "%~dp0.."
call mvn clean package -DskipTests
set "build_exit=%errorlevel%"
popd
exit /b %build_exit%
