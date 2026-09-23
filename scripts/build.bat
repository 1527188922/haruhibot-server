@echo off
setlocal
pushd "%~dp0..\webui"
call npm run build
if errorlevel 1 (popd & exit /b 1)
popd
call "%~dp0build-back.bat"
exit /b %errorlevel%
